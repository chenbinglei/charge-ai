package com.elink.evco.platform.iam.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.elink.evco.platform.common.cache.RedisKeys;
import com.elink.evco.platform.common.config.AppProperties;
import com.elink.evco.platform.common.security.AuthContext;
import com.elink.evco.platform.common.security.JwtTokenProvider;
import com.elink.evco.platform.iam.entity.AuthSession;
import com.elink.evco.platform.iam.entity.IamUser;
import com.elink.evco.platform.iam.entity.RefreshToken;
import com.elink.evco.platform.iam.mapper.AuthSessionMapper;
import com.elink.evco.platform.iam.mapper.RefreshTokenMapper;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.List;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 会话与令牌管理：登录签发（auth_session + refresh_token + JWT access_token）、
 * 有效性校验（Redis 快路径 + DB 回源）、滑动续期与批量撤销。
 *
 * <p>Redis 会话键 TTL 与 access_token 对齐；键过期后由 DB 会话状态兜底校验，
 * 保证 refresh 长周期内令牌仍可恢复。
 */
@Service
public class SessionService {

    /** 签发的令牌三元组。 */
    public record IssuedTokens(String accessToken, String refreshToken, long expiresInSeconds, Long sessionId) {}

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
     * 构造会话服务。
     *
     * @param sessionMapper 会话 Mapper。
     * @param refreshTokenMapper 刷新令牌 Mapper。
     * @param jwtTokenProvider JWT 签发器。
     * @param redis Redis 客户端。
     * @param keys Redis 键规则。
     * @param appProperties 平台业务配置。
     */
    public SessionService(
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
     * @param user 登录用户。
     * @param authType 认证方式：PASSWORD/SSO。
     * @param ip 登录来源 IP。
     * @param userAgent 登录端 User-Agent 摘要。
     * @return 令牌三元组（含会话 ID）。
     */
    @Transactional
    public IssuedTokens issue(IamUser user, String authType, String ip, String userAgent) {
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        AuthSession session = new AuthSession();
        session.setUserId(user.getId());
        session.setAuthType(authType);
        session.setStatus(AuthSession.STATUS_ACTIVE);
        session.setIp(ip);
        session.setUserAgent(truncate(userAgent));
        session.setExpiresAt(now.plusDays(appProperties.getAuth().getRefreshTokenTtlDays()));
        sessionMapper.insert(session);

        String refreshToken = newRefreshTokenPlain();
        RefreshToken entity = new RefreshToken();
        entity.setSessionId(session.getId());
        entity.setUserId(user.getId());
        entity.setTokenHash(sha256Hex(refreshToken));
        entity.setStatus(RefreshToken.STATUS_ACTIVE);
        entity.setExpiresAt(now.plusDays(appProperties.getAuth().getRefreshTokenTtlDays()));
        refreshTokenMapper.insert(entity);

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
                accessToken, refreshToken, Duration.ofMinutes(jwtTokenProvider.accessTokenTtlMinutes()).toSeconds(),
                session.getId());
    }

    /**
     * 校验会话是否有效；Redis 快路径，键缺失时回源数据库并回填。
     *
     * @param sessionId 会话 ID。
     * @return 有效时返回用户 ID；无效返回 null。
     */
    public Long validateActive(Long sessionId) {
        if (sessionId == null) {
            return null;
        }
        try {
            String userId = redis.opsForValue().get(keys.session(sessionId));
            if (userId != null) {
                return Long.parseLong(userId);
            }
        } catch (RuntimeException ignored) {
            // Redis 故障时回源数据库，鉴权不因缓存不可用而误拒。
        }
        AuthSession session =
                sessionMapper.selectOne(
                        new LambdaQueryWrapper<AuthSession>()
                                .eq(AuthSession::getId, sessionId)
                                .eq(AuthSession::getStatus, AuthSession.STATUS_ACTIVE)
                                .gt(AuthSession::getExpiresAt, LocalDateTime.now(ZoneOffset.UTC)));
        if (session == null) {
            return null;
        }
        touchRedis(session.getId(), session.getUserId());
        return session.getUserId();
    }

    /**
     * 撤销指定用户的全部有效会话与刷新令牌；停用/锁定/删除/强制下线时调用。
     *
     * @param userId 用户 ID。
     */
    @Transactional
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
     * @param sessionId 会话 ID。
     */
    @Transactional
    public void revokeSession(Long sessionId) {
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        sessionMapper.update(
                null,
                new LambdaUpdateWrapper<AuthSession>()
                        .eq(AuthSession::getId, sessionId)
                        .eq(AuthSession::getStatus, AuthSession.STATUS_ACTIVE)
                        .set(AuthSession::getStatus, AuthSession.STATUS_REVOKED)
                        .set(AuthSession::getUpdatedAt, now));
        refreshTokenMapper.update(
                null,
                new LambdaUpdateWrapper<RefreshToken>()
                        .eq(RefreshToken::getSessionId, sessionId)
                        .eq(RefreshToken::getStatus, RefreshToken.STATUS_ACTIVE)
                        .set(RefreshToken::getStatus, RefreshToken.STATUS_REVOKED)
                        .set(RefreshToken::getUpdatedAt, now));
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
    public void slide(Long sessionId) {
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        sessionMapper.update(
                null,
                new LambdaUpdateWrapper<AuthSession>()
                        .eq(AuthSession::getId, sessionId)
                        .set(AuthSession::getExpiresAt, now.plusDays(appProperties.getAuth().getRefreshTokenTtlDays()))
                        .set(AuthSession::getUpdatedAt, now));
        try {
            redis.expire(keys.session(sessionId), Duration.ofMinutes(jwtTokenProvider.accessTokenTtlMinutes()));
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
     * 计算 SHA-256 十六进制摘要；刷新令牌仅存摘要。
     *
     * @param value 原文。
     * @return 十六进制摘要。
     */
    public static String sha256Hex(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                hex.append(Character.forDigit((b >> 4) & 0xF, 16)).append(Character.forDigit(b & 0xF, 16));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 算法不可用", ex);
        }
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
