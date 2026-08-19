package com.elink.evco.platform.iam.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.elink.evco.platform.iam.cache.RedisKeys;
import com.elink.evco.platform.iam.entity.AuthSession;
import com.elink.evco.platform.iam.entity.IamUser;
import com.elink.evco.platform.iam.entity.RefreshToken;
import com.elink.evco.platform.iam.mapper.AuthSessionMapper;
import com.elink.evco.platform.iam.mapper.RefreshTokenMapper;
import com.elink.evco.platform.iam.service.SessionService;
import com.elink.evco.platform.iam.util.HashUtils;
import com.elink.evco.web.config.AppProperties;
import com.elink.evco.web.security.AuthContext;
import com.elink.evco.web.security.JwtTokenProvider;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.List;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 会话与令牌实现：auth_session + refresh_token 双表落库，Redis 快路径校验， DB 兜底回源；刷新令牌仅存 SHA-256 摘要。 */
@Service
public class SessionServiceImpl implements SessionService {

    /** 安全随机源；刷新令牌原文生成。 */
    private final SecureRandom random = new SecureRandom();

    /** 会话数据访问。 */
    private final AuthSessionMapper sessionMapper;

    /** 刷新令牌数据访问。 */
    private final RefreshTokenMapper refreshTokenMapper;

    /** JWT 签发器。 */
    private final JwtTokenProvider jwtTokenProvider;

    /** Redis 客户端。 */
    private final StringRedisTemplate redis;

    /** Redis 键规则。 */
    private final RedisKeys keys;

    /** 业务配置。 */
    private final AppProperties appProperties;

    /**
     * 构造会话实现。
     *
     * @param sessionMapper 会话 Mapper。
     * @param refreshTokenMapper 刷新令牌 Mapper。
     * @param jwtTokenProvider JWT 签发器。
     * @param redis Redis 客户端。
     * @param keys Redis 键规则。
     * @param appProperties 平台业务配置。
     */
    public SessionServiceImpl(
            AuthSessionMapper sessionMapper,
            RefreshTokenMapper refreshTokenMapper,
            JwtTokenProvider jwtTokenProvider,
            StringRedisTemplate redis,
            RedisKeys keys,
            AppProperties appProperties) {
        this.sessionMapper = sessionMapper;
        this.refreshTokenMapper = refreshTokenMapper;
        this.jwtTokenProvider = jwtTokenProvider;
        this.redis = redis;
        this.keys = keys;
        this.appProperties = appProperties;
    }

    /**
     * 为登录成功的用户建立会话并签发令牌对。
     *
     * <p>步骤：落会话 → 落刷新令牌（仅存摘要）→ 签发 JWT → 回填 Redis 快路径键。
     *
     * @param user 登录用户。
     * @param authType 认证方式：PASSWORD/SSO。
     * @param ip 登录来源 IP。
     * @param userAgent 登录端 User-Agent 摘要。
     * @return 令牌三元组（含会话 ID）。
     */
    @Transactional
    @Override
    public IssuedTokens issue(IamUser user, String authType, String ip, String userAgent) {
        // 1. 建立 auth_session 会话记录。
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        AuthSession session = new AuthSession();
        session.setUserId(user.getId());
        session.setAuthType(authType);
        session.setStatus(AuthSession.STATUS_ACTIVE);
        session.setIp(ip);
        session.setUserAgent(truncate(userAgent));
        session.setExpiresAt(now.plusDays(appProperties.getAuth().getRefreshTokenTtlDays()));
        sessionMapper.insert(session);

        // 2. 生成刷新令牌原文并落摘要记录。
        String refreshToken = newRefreshTokenPlain();
        RefreshToken entity = new RefreshToken();
        entity.setSessionId(session.getId());
        entity.setUserId(user.getId());
        entity.setTokenHash(HashUtils.sha256Hex(refreshToken));
        entity.setStatus(RefreshToken.STATUS_ACTIVE);
        entity.setExpiresAt(now.plusDays(appProperties.getAuth().getRefreshTokenTtlDays()));
        refreshTokenMapper.insert(entity);

        // 3. 签发 JWT access_token 并回填 Redis 会话键。
        AuthContext context =
                new AuthContext(
                        user.getId(),
                        session.getId(),
                        user.getUsername(),
                        user.getDisplayName(),
                        user.getTenantId(),
                        user.getUserType(),
                        authType);
        String accessToken = jwtTokenProvider.createAccessToken(context);
        touchRedis(session.getId(), user.getId());
        return new IssuedTokens(
                accessToken,
                refreshToken,
                Duration.ofMinutes(jwtTokenProvider.accessTokenTtlMinutes()).toSeconds(),
                session.getId());
    }

    /**
     * 校验会话是否有效；Redis 快路径，键缺失时回源数据库并回填。
     *
     * <p>步骤：Redis 快路径命中 → DB 回源（active + 未过期）→ 回填 Redis。
     *
     * @param sessionId 会话 ID。
     * @return 有效时返回用户 ID；无效返回 null。
     */
    @Override
    public Long validateActive(Long sessionId) {
        if (sessionId == null) {
            return null;
        }
        // 1. Redis 快路径。
        try {
            String userId = redis.opsForValue().get(keys.session(sessionId));
            if (userId != null) {
                return Long.parseLong(userId);
            }
        } catch (RuntimeException ignored) {
            // Redis 故障时回源数据库，鉴权不因缓存不可用而误拒。
        }
        // 2. DB 回源：active 且未过期。
        AuthSession session =
                sessionMapper.selectOne(
                        new LambdaQueryWrapper<AuthSession>()
                                .eq(AuthSession::getId, sessionId)
                                .eq(AuthSession::getStatus, AuthSession.STATUS_ACTIVE)
                                .gt(AuthSession::getExpiresAt, LocalDateTime.now(ZoneOffset.UTC)));
        if (session == null) {
            return null;
        }
        // 3. 回填 Redis 快路径键。
        touchRedis(session.getId(), session.getUserId());
        return session.getUserId();
    }

    /**
     * 撤销指定用户的全部有效会话与刷新令牌；停用/锁定/删除/强制下线时调用。
     *
     * @param userId 用户 ID。
     */
    @Transactional
    @Override
    public void revokeByUser(Long userId) {
        List<AuthSession> sessions =
                sessionMapper.selectList(
                        new LambdaQueryWrapper<AuthSession>()
                                .eq(AuthSession::getUserId, userId)
                                .eq(AuthSession::getStatus, AuthSession.STATUS_ACTIVE));
        sessions.forEach(session -> revokeSession(session.getId()));
    }

    /**
     * 撤销单个会话及其刷新令牌；登出与被动撤销共用。
     *
     * <p>步骤：会话置 revoked → 刷新令牌置 revoked → 清 Redis 键（失败不阻断）。
     *
     * @param sessionId 会话 ID。
     */
    @Transactional
    @Override
    public void revokeSession(Long sessionId) {
        // 1. 会话置 revoked。
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        sessionMapper.update(
                null,
                new LambdaUpdateWrapper<AuthSession>()
                        .eq(AuthSession::getId, sessionId)
                        .eq(AuthSession::getStatus, AuthSession.STATUS_ACTIVE)
                        .set(AuthSession::getStatus, AuthSession.STATUS_REVOKED)
                        .set(AuthSession::getUpdatedAt, now));
        // 2. 关联刷新令牌置 revoked。
        refreshTokenMapper.update(
                null,
                new LambdaUpdateWrapper<RefreshToken>()
                        .eq(RefreshToken::getSessionId, sessionId)
                        .eq(RefreshToken::getStatus, RefreshToken.STATUS_ACTIVE)
                        .set(RefreshToken::getStatus, RefreshToken.STATUS_REVOKED)
                        .set(RefreshToken::getUpdatedAt, now));
        // 3. 清 Redis 键；失败由 TTL 自然过期兜底。
        try {
            redis.delete(keys.session(sessionId));
        } catch (RuntimeException ignored) {
            // Redis 清理失败不影响撤销语义；会话键 TTL 会自然过期。
        }
    }

    /**
     * 滑动续期：延长会话过期时间并刷新 Redis 键 TTL；refresh 流程调用。
     *
     * @param sessionId 会话 ID。
     */
    @Override
    public void slide(Long sessionId) {
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        sessionMapper.update(
                null,
                new LambdaUpdateWrapper<AuthSession>()
                        .eq(AuthSession::getId, sessionId)
                        .set(
                                AuthSession::getExpiresAt,
                                now.plusDays(appProperties.getAuth().getRefreshTokenTtlDays()))
                        .set(AuthSession::getUpdatedAt, now));
        try {
            redis.expire(
                    keys.session(sessionId),
                    Duration.ofMinutes(jwtTokenProvider.accessTokenTtlMinutes()));
        } catch (RuntimeException ignored) {
            // Redis 故障时仅影响快路径，DB 过期时间已续期。
        }
    }

    /**
     * 回填 Redis 会话键（TTL 与 access_token 对齐）。
     *
     * @param sessionId 会话 ID。
     * @param userId 用户 ID。
     */
    private void touchRedis(Long sessionId, Long userId) {
        try {
            redis.opsForValue()
                    .set(
                            keys.session(sessionId),
                            String.valueOf(userId),
                            Duration.ofMinutes(jwtTokenProvider.accessTokenTtlMinutes()));
        } catch (RuntimeException ignored) {
            // Redis 故障时回源数据库即可；不阻塞登录。
        }
    }

    /**
     * 生成 32 字节随机刷新令牌原文（Base64URL）。
     *
     * @return 令牌原文。
     */
    private String newRefreshTokenPlain() {
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    /**
     * 截断 User-Agent 至字段长度上限。
     *
     * @param userAgent 原始值。
     * @return 截断后的值；null 安全。
     */
    private String truncate(String userAgent) {
        if (userAgent == null) {
            return null;
        }
        return userAgent.length() <= 255 ? userAgent : userAgent.substring(0, 255);
    }
}
