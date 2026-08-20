package com.elink.evco.platform.iam.service.impl;

import com.elink.evco.platform.iam.cache.RedisKeys;
import com.elink.evco.platform.iam.dto.SsoTicketPushRequest;
import com.elink.evco.platform.iam.entity.AuthSession;
import com.elink.evco.platform.iam.entity.IamAuditLog;
import com.elink.evco.platform.iam.entity.IamRole;
import com.elink.evco.platform.iam.entity.IamUser;
import com.elink.evco.platform.iam.mapper.IamRoleMapper;
import com.elink.evco.platform.iam.mapper.IamUserMapper;
import com.elink.evco.platform.iam.service.AuditService;
import com.elink.evco.platform.iam.service.SessionService;
import com.elink.evco.platform.iam.service.SsoService;
import com.elink.evco.platform.iam.vo.LoginVO;
import com.elink.evco.web.config.AppProperties;
import com.elink.evco.web.config.DeploymentMode;
import com.elink.evco.web.error.BusinessException;
import com.elink.evco.web.error.PlatformErrorCode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

/** SSO 免登录实现：ticket 唯一写入 + GETDEL 一次性消费 + 双重模式与凭证校验。 */
@Service
public class SsoServiceImpl implements SsoService {

    /** 受控日志；ticket 指纹记录。 */
    private static final Logger LOG = LoggerFactory.getLogger(SsoServiceImpl.class);

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
     * 构造 SSO 实现。
     *
     * @param userMapper 用户 Mapper。
     * @param roleMapper 角色 Mapper。
     * @param sessionService 会话服务。
     * @param auditService 审计服务。
     * @param redis Redis 客户端。
     * @param keys Redis 键规则。
     * @param appProperties 平台业务配置。
     */
    public SsoServiceImpl(
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
     * <p>步骤：模式断言 → X-API-Key 常量时间比对 → 过期校验 → SET EX NX 唯一写入（Redis 故障 fail-closed）。
     *
     * @param request 推送请求（ticket、iotUserId、expireAt）。
     * @param apiKeyHeader 请求携带的 X-API-Key。
     */
    @Override
    public void pushTicket(SsoTicketPushRequest request, String apiKeyHeader) {
        // 1. 仅 IOT 协同模式开放推送入口。
        requireIotMode("SSO ticket 推送入口关闭");
        // 2. 服务间凭证常量时间比对；未配置直接拒绝（fail-closed）。
        String expected = appProperties.getAuth().getSso().getApiKey();
        if (expected == null || expected.isBlank()) {
            // fail-closed：未配置服务间凭证时拒绝一切推送。
            throw new BusinessException(PlatformErrorCode.UNAUTHENTICATED, "服务间凭证未配置");
        }
        if (apiKeyHeader == null
                || !MessageDigest.isEqual(
                        expected.getBytes(StandardCharsets.UTF_8),
                        apiKeyHeader.getBytes(StandardCharsets.UTF_8))) {
            throw new BusinessException(PlatformErrorCode.UNAUTHENTICATED, "服务间凭证无效");
        }
        // 3. ticket 过期前置校验。
        if (request.expireAt().isBefore(Instant.now())) {
            throw new BusinessException(PlatformErrorCode.VALIDATION_ERROR, "ticket 已过期");
        }
        // 4. SET EX NX 唯一写入；冲突即重复 ticket，Redis 故障 fail-closed 不接收。
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
     * SSO 免登录：ticket 换会话；仅识别已推送用户（source=IOT_PUSH、status=active）， 本地不存在或不可用返回
     * SSO_USER_NOT_FOUND（ticket 保持已消费，不回滚复活）。
     *
     * <p>步骤：模式断言 → GETDEL 一次性消费 → 定位 IOT_PUSH 用户 → 签发会话 → 审计 → 组装登录响应。
     *
     * @param ticket 一次性票据原文。
     * @param ip 来源 IP。
     * @param userAgent 登录端 User-Agent。
     * @return 登录响应（与密码登录令牌结构一致）。
     */
    @Override
    public LoginVO ssoLogin(String ticket, String ip, String userAgent) {
        // 1. 仅 IOT 协同模式开放免登录入口。
        requireIotMode("SSO 免登录入口关闭");
        // 2. GETDEL 原子消费；不存在/已用/已过期统一拒绝，防探测。
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
        // 3. 定位已推送且 active 的本地用户：IOT 推送的用户 ID 即本表主键，直接按主键查。
        IamUser user = userMapper.selectById(Long.parseLong(iotUserId.trim()));
        if (user == null
                || user.getDeletedAt() != null
                || !IamUser.SOURCE_IOT_PUSH.equals(user.getSource())
                || !IamUser.STATUS_ACTIVE.equals(user.getStatus())) {
            throw new BusinessException(PlatformErrorCode.SSO_USER_NOT_FOUND);
        }
        // 4. 签发会话（SSO 类型）并落审计。
        SessionService.IssuedTokens tokens =
                sessionService.issue(user, AuthSession.AUTH_TYPE_SSO, ip, userAgent);
        auditService.record(
                user.getId(),
                user.getTenantId(),
                IamAuditLog.ACTION_LOGIN,
                "iam_user",
                user.getId(),
                "用户 SSO 免登录成功 fingerprint=" + fingerprint8(ticket),
                ip);
        // 5. 组装与密码登录一致的响应结构。
        List<String> roleNames =
                roleMapper.selectByUserId(user.getId()).stream().map(IamRole::getName).toList();
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
        } catch (Exception ex) {
            return "unavailable";
        }
    }
}
