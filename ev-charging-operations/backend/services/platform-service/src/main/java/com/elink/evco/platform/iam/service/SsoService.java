package com.elink.evco.platform.iam.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.elink.evco.platform.common.cache.RedisKeys;
import com.elink.evco.platform.common.config.AppProperties;
import com.elink.evco.platform.common.config.DeploymentMode;
import com.elink.evco.platform.common.error.BusinessException;
import com.elink.evco.platform.common.error.PlatformErrorCode;
import com.elink.evco.platform.iam.dto.SsoTicketPushRequest;
import com.elink.evco.platform.iam.entity.AuthSession;
import com.elink.evco.platform.iam.entity.IamAuditLog;
import com.elink.evco.platform.iam.entity.IamRole;
import com.elink.evco.platform.iam.entity.IamUser;
import com.elink.evco.platform.iam.mapper.IamRoleMapper;
import com.elink.evco.platform.iam.mapper.IamUserMapper;
import com.elink.evco.platform.iam.vo.LoginVO;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

/**
 * SSO 免登录服务（仅 IOT 协同模式启用）：
 * IOT 推送一次性 ticket（SET EX 120 NX 唯一写入）→ 前端跳转 /sso/login →
 * GETDEL 原子消费换会话；并发同 ticket 有且仅有一个成功，防重放；
 * Redis 故障 fail-closed；ticket 原文不落日志/审计（仅 SHA-256 指纹前 8 位）。
 */
@Service
public class SsoService {

    /** 受控日志；ticket 指纹记录。 */
    private static final Logger LOG = LoggerFactory.getLogger(SsoService.class);

    /** ticket Redis TTL（秒）。 */
    private static final int TICKET_TTL_SECONDS = 120;

    /** 用户数据访问。 */
    private final IamUserMapper userMapper;

    /** 角色数据访问。 */
    private final IamRoleMapper roleMapper;

    /** 会话与令牌管理。 */
    private final SessionService sessionService;

    /** 审计服务。 */
    private final AuditService auditService;

    /** Redis 客户端。 */
    private final StringRedisTemplate redis;

    /** Redis 键规则。 */
    private final RedisKeys keys;

    /** 业务配置。 */
    private final AppProperties appProperties;

    /**
     * 构造 SSO 服务。
     *
     * @param userMapper 用户 Mapper。
     * @param roleMapper 角色 Mapper。
     * @param sessionService 会话服务。
     * @param auditService 审计服务。
     * @param redis Redis 客户端。
     * @param keys Redis 键规则。
     * @param appProperties 平台业务配置。
     */
    public SsoService(
            IamUserMapper userMapper,
            IamRoleMapper roleMapper,
            SessionService sessionService,
            AuditService auditService,
            StringRedisTemplate redis,
            RedisKeys keys,
            AppProperties appProperties) {
        this.userMapper = userMapper;
        this.roleMapper = roleMapper;
        this.sessionService = sessionService;
        this.auditService = auditService;
        this.redis = redis;
        this.keys = keys;
        this.appProperties = appProperties;
    }

    /**
     * IOT 推送一次性 SSO ticket（服务间接口）；仅 IOT 协同模式启用。
     *
     * @param request 推送请求（ticket、iotUserId、expireAt）。
     * @param apiKeyHeader 请求携带的 X-API-Key。
     */
    public void pushTicket(SsoTicketPushRequest request, String apiKeyHeader) {
        requireIotMode("SSO ticket 推送入口关闭");
        String expected = appProperties.getAuth().getSso().getApiKey();
        if (expected == null || expected.isBlank()) {
            // fail-closed：未配置服务间凭证时拒绝一切推送。
            throw new BusinessException(PlatformErrorCode.UNAUTHENTICATED, "服务间凭证未配置");
        }
        if (apiKeyHeader == null || !MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.UTF_8), apiKeyHeader.getBytes(StandardCharsets.UTF_8))) {
            throw new BusinessException(PlatformErrorCode.UNAUTHENTICATED, "服务间凭证无效");
        }
        if (request.expireAt().isBefore(Instant.now())) {
            throw new BusinessException(PlatformErrorCode.VALIDATION_ERROR, "ticket 已过期");
        }
        try {
            Boolean stored =
                    redis.opsForValue()
                            .setIfAbsent(
                                    keys.ssoTicket(request.ticket()),
                                    request.iotUserId(),
                                    Duration.ofSeconds(TICKET_TTL_SECONDS));
            if (!Boolean.TRUE.equals(stored)) {
                throw new BusinessException(PlatformErrorCode.VALIDATION_ERROR, "ticket 已存在");
            }
        } catch (BusinessException ex) {
            throw ex;
        } catch (RuntimeException ex) {
            // Redis 故障 fail-closed：不接收无法保证一次性消费语义的 ticket。
            throw new BusinessException(PlatformErrorCode.INTERNAL_ERROR, "缓存服务不可用，ticket 未接收");
        }
        LOG.info(
                "SSO ticket 已接收 fingerprint={} iotUserId={}",
                fingerprint8(request.ticket()),
                request.iotUserId());
    }

    /**
     * SSO 免登录：ticket 换会话；仅识别已推送用户（source=IOT_PUSH、status=active），
     * 本地不存在或不可用返回 SSO_USER_NOT_FOUND（ticket 保持已消费，不回滚复活）。
     *
     * @param ticket 一次性票据原文。
     * @param ip 来源 IP。
     * @param userAgent 登录端 User-Agent。
     * @return 登录响应（与密码登录令牌结构一致）。
     */
    public LoginVO ssoLogin(String ticket, String ip, String userAgent) {
        requireIotMode("SSO 免登录入口关闭");
        String iotUserId;
        try {
            iotUserId = redis.opsForValue().getAndDelete(keys.ssoTicket(ticket));
        } catch (RuntimeException ex) {
            throw new BusinessException(PlatformErrorCode.INTERNAL_ERROR, "缓存服务不可用，SSO 登录拒绝");
        }
        if (iotUserId == null) {
            // 不区分不存在/已用/已过期，防探测。
            throw new BusinessException(PlatformErrorCode.SSO_TICKET_INVALID);
        }
        IamUser user =
                userMapper.selectOne(
                        new LambdaQueryWrapper<IamUser>()
                                .eq(IamUser::getIotUserId, Long.parseLong(iotUserId.trim()))
                                .eq(IamUser::getSource, IamUser.SOURCE_IOT_PUSH)
                                .isNull(IamUser::getDeletedAt));
        if (user == null || !IamUser.STATUS_ACTIVE.equals(user.getStatus())) {
            throw new BusinessException(PlatformErrorCode.SSO_USER_NOT_FOUND);
        }
        SessionService.IssuedTokens tokens = sessionService.issue(user, AuthSession.AUTH_TYPE_SSO, ip, userAgent);
        auditService.record(
                user.getId(),
                user.getTenantId(),
                IamAuditLog.ACTION_LOGIN,
                "iam_user",
                user.getId(),
                "用户 SSO 免登录成功 fingerprint=" + fingerprint8(ticket),
                ip);
        List<String> roleNames =
                roleMapper.selectByUserId(user.getId()).stream()
                        .map(IamRole::getName)
                        .toList();
        return new LoginVO(
                tokens.accessToken(),
                tokens.refreshToken(),
                tokens.expiresInSeconds(),
                "Bearer",
                String.valueOf(user.getId()),
                user.getDisplayName(),
                roleNames,
                appProperties.getDeploymentMode().name());
    }

    /**
     * 断言当前为 IOT 协同模式；独立部署模式下 SSO 入口整体关闭。
     *
     * @param closedMessage 关闭提示。
     */
    private void requireIotMode(String closedMessage) {
        if (appProperties.getDeploymentMode() != DeploymentMode.IOT_COLLABORATIVE) {
            throw new BusinessException(PlatformErrorCode.FORBIDDEN, closedMessage);
        }
    }

    /**
     * 计算 ticket SHA-256 指纹前 8 位；用于日志与审计，不暴露原文。
     *
     * @param ticket 票据原文。
     * @return 指纹前 8 位十六进制。
     */
    private String fingerprint8(String ticket) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(ticket.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (int i = 0; i < 4; i++) {
                hex.append(String.format("%02x", hash[i]));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException ex) {
            return "unavailable";
        }
    }
}
