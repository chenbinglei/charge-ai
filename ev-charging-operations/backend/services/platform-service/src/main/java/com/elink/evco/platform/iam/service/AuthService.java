package com.elink.evco.platform.iam.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.elink.evco.platform.common.config.AppProperties;
import com.elink.evco.platform.common.error.BusinessException;
import com.elink.evco.platform.common.error.PlatformErrorCode;
import com.elink.evco.platform.iam.dto.LoginRequest;
import com.elink.evco.platform.iam.entity.AuthSession;
import com.elink.evco.platform.iam.entity.IamAuditLog;
import com.elink.evco.platform.iam.entity.IamRole;
import com.elink.evco.platform.iam.entity.IamUser;
import com.elink.evco.platform.iam.entity.RefreshToken;
import com.elink.evco.platform.iam.mapper.AuthSessionMapper;
import com.elink.evco.platform.iam.mapper.IamRoleMapper;
import com.elink.evco.platform.iam.mapper.IamUserMapper;
import com.elink.evco.platform.iam.mapper.RefreshTokenMapper;
import com.elink.evco.platform.iam.vo.LoginVO;
import com.elink.evco.platform.iam.vo.MenuNodeVO;
import com.elink.evco.platform.iam.vo.ProfileVO;
import com.elink.evco.platform.common.security.AuthContext;
import com.elink.evco.platform.common.security.JwtTokenProvider;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * 认证服务：图形验证码登录（5 次失败锁 30 分钟）、登出、刷新（一次性 refresh_token
 * 轮换）、被动撤销与 profile（权限/菜单/部署模式）。
 *
 * <p>失败路径不区分「用户不存在」与「密码错误」；锁定/停用状态分别提示但不泄露其他账号信息。
 */
@Service
public class AuthService {

    /** 用户数据访问。 */
    private final IamUserMapper userMapper;

    /** 角色数据访问。 */
    private final IamRoleMapper roleMapper;

    /** 会话数据访问。 */
    private final AuthSessionMapper sessionMapper;

    /** 刷新令牌数据访问。 */
    private final RefreshTokenMapper refreshTokenMapper;

    /** 会话与令牌管理。 */
    private final SessionService sessionService;

    /** 图形验证码服务。 */
    private final CaptchaService captchaService;

    /** 权限读取服务。 */
    private final PermissionService permissionService;

    /** 菜单推导服务。 */
    private final MenuService menuService;

    /** 审计服务。 */
    private final AuditService auditService;

    /** JWT 签发器。 */
    private final JwtTokenProvider jwtTokenProvider;

    /** 密码编码器（BCrypt）。 */
    private final PasswordEncoder passwordEncoder;

    /** 业务配置。 */
    private final AppProperties appProperties;

    /**
     * 构造认证服务。
     *
     * @param userMapper 用户 Mapper。
     * @param roleMapper 角色 Mapper。
     * @param sessionMapper 会话 Mapper。
     * @param refreshTokenMapper 刷新令牌 Mapper。
     * @param sessionService 会话服务。
     * @param captchaService 验证码服务。
     * @param permissionService 权限服务。
     * @param menuService 菜单服务。
     * @param auditService 审计服务。
     * @param jwtTokenProvider JWT 签发器。
     * @param passwordEncoder 密码编码器。
     * @param appProperties 平台业务配置。
     */
    public AuthService(
            IamUserMapper userMapper,
            IamRoleMapper roleMapper,
            AuthSessionMapper sessionMapper,
            RefreshTokenMapper refreshTokenMapper,
            SessionService sessionService,
            CaptchaService captchaService,
            PermissionService permissionService,
            MenuService menuService,
            AuditService auditService,
            JwtTokenProvider jwtTokenProvider,
            PasswordEncoder passwordEncoder,
            AppProperties appProperties) {
        this.userMapper = userMapper;
        this.roleMapper = roleMapper;
        this.sessionMapper = sessionMapper;
        this.refreshTokenMapper = refreshTokenMapper;
        this.sessionService = sessionService;
        this.captchaService = captchaService;
        this.permissionService = permissionService;
        this.menuService = menuService;
        this.auditService = auditService;
        this.jwtTokenProvider = jwtTokenProvider;
        this.passwordEncoder = passwordEncoder;
        this.appProperties = appProperties;
    }

    /**
     * 管理端账号密码登录；验证码一次性校验，成功签发令牌对并回写登录事实。
     *
     * @param request 登录请求。
     * @param ip 来源 IP。
     * @param userAgent 登录端 User-Agent。
     * @return 登录响应（令牌对、角色、部署模式）。
     */
    public LoginVO login(LoginRequest request, String ip, String userAgent) {
        if (!captchaService.verify(request.captchaId(), request.captchaCode())) {
            throw new BusinessException(PlatformErrorCode.UNAUTHENTICATED, "验证码错误或已过期");
        }
        LambdaQueryWrapper<IamUser> wrapper =
                new LambdaQueryWrapper<IamUser>()
                        .eq(IamUser::getUsername, request.username().trim())
                        .isNull(IamUser::getDeletedAt);
        if (request.tenantId() != null && !request.tenantId().isBlank()) {
            wrapper.eq(IamUser::getTenantId, Long.parseLong(request.tenantId().trim()));
        }
        IamUser user = userMapper.selectOne(wrapper);
        if (user == null) {
            throw new BusinessException(PlatformErrorCode.UNAUTHENTICATED, "用户名或密码错误");
        }
        ensureLoginAllowed(user);
        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            recordPasswordFailure(user);
            throw new BusinessException(PlatformErrorCode.UNAUTHENTICATED, "用户名或密码错误");
        }
        return completeLogin(user, AuthSession.AUTH_TYPE_PASSWORD, ip, userAgent);
    }

    /**
     * 登录成功公共路径：清零失败计数、回写登录事实、签发令牌并审计。
     *
     * @param user 用户实体。
     * @param authType 认证方式。
     * @param ip 来源 IP。
     * @param userAgent User-Agent。
     * @return 登录响应。
     */
    private LoginVO completeLogin(IamUser user, String authType, String ip, String userAgent) {
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        userMapper.update(
                null,
                new LambdaUpdateWrapper<IamUser>()
                        .eq(IamUser::getId, user.getId())
                        .set(IamUser::getFailCount, 0)
                        .set(IamUser::getFailWindowStart, null)
                        .set(IamUser::getLockedUntil, null)
                        .set(IamUser::getLastLoginAt, now)
                        .set(IamUser::getLastLoginIp, ip)
                        .set(IamUser::getUpdatedAt, now));
        SessionService.IssuedTokens tokens = sessionService.issue(user, authType, ip, userAgent);
        auditService.record(
                user.getId(),
                user.getTenantId(),
                IamAuditLog.ACTION_LOGIN,
                "iam_user",
                user.getId(),
                "用户登录成功（" + authType + "）",
                ip);
        List<String> roleNames = roleMapper.selectByUserId(user.getId()).stream().map(IamRole::getName).toList();
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
     * 登录前置状态校验：停用拒绝、锁定期拒绝（期满自动解锁）、到期账号自动锁定。
     *
     * @param user 用户实体。
     */
    private void ensureLoginAllowed(IamUser user) {
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        if (IamUser.STATUS_DISABLED.equals(user.getStatus())) {
            throw new BusinessException(PlatformErrorCode.UNAUTHENTICATED, "账号已停用，请联系管理员");
        }
        if (IamUser.STATUS_LOCKED.equals(user.getStatus())) {
            if (user.getLockedUntil() != null && now.isBefore(user.getLockedUntil())) {
                long minutes = Duration.between(now, user.getLockedUntil()).toMinutes() + 1;
                throw new BusinessException(PlatformErrorCode.UNAUTHENTICATED, "账号已锁定，请约 " + minutes + " 分钟后重试");
            }
            if (user.getLockedUntil() == null) {
                throw new BusinessException(PlatformErrorCode.UNAUTHENTICATED, "账号已锁定，请联系管理员");
            }
            // 锁定期已过：自动解锁后继续认证。
            userMapper.update(
                    null,
                    new LambdaUpdateWrapper<IamUser>()
                            .eq(IamUser::getId, user.getId())
                            .set(IamUser::getStatus, IamUser.STATUS_ACTIVE)
                            .set(IamUser::getUpdatedAt, now));
            user.setStatus(IamUser.STATUS_ACTIVE);
        }
        if (user.getExpireAt() != null && !now.isBefore(user.getExpireAt())) {
            userMapper.update(
                    null,
                    new LambdaUpdateWrapper<IamUser>()
                            .eq(IamUser::getId, user.getId())
                            .set(IamUser::getStatus, IamUser.STATUS_LOCKED)
                            .set(IamUser::getLockedUntil, null)
                            .set(IamUser::getUpdatedAt, now));
            auditService.record(
                    null,
                    user.getTenantId(),
                    IamAuditLog.ACTION_STATUS_CHANGE,
                    "iam_user",
                    user.getId(),
                    "账号到期自动锁定",
                    null);
            throw new BusinessException(PlatformErrorCode.UNAUTHENTICATED, "账号已到期，已自动锁定");
        }
    }

    /**
     * 记录密码失败：窗口内累计，达到上限锁定 30 分钟。
     *
     * @param user 用户实体。
     */
    private void recordPasswordFailure(IamUser user) {
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        int windowMinutes = appProperties.getAuth().getLockout().getLockMinutes();
        int maxFails = appProperties.getAuth().getLockout().getMaxFails();
        boolean windowValid =
                user.getFailWindowStart() != null
                        && Duration.between(user.getFailWindowStart(), now).toMinutes() < windowMinutes;
        int nextCount = windowValid ? user.getFailCount() + 1 : 1;
        boolean lock = nextCount >= maxFails;
        LambdaUpdateWrapper<IamUser> wrapper =
                new LambdaUpdateWrapper<IamUser>()
                        .eq(IamUser::getId, user.getId())
                        .set(IamUser::getFailCount, nextCount)
                        .set(IamUser::getFailWindowStart, windowValid ? user.getFailWindowStart() : now)
                        .set(IamUser::getUpdatedAt, now);
        if (lock) {
            wrapper.set(IamUser::getStatus, IamUser.STATUS_LOCKED)
                    .set(IamUser::getLockedUntil, now.plusMinutes(windowMinutes));
        }
        userMapper.update(null, wrapper);
    }

    /**
     * 主动登出：撤销会话与刷新令牌，清理权限缓存并审计。
     *
     * @param refreshToken 刷新令牌原文。
     */
    public void logout(String refreshToken) {
        RefreshToken token = loadByHash(refreshToken);
        if (token == null) {
            return;
        }
        sessionService.revokeSession(token.getSessionId());
        permissionService.evict(token.getUserId());
        auditService.record(
                token.getUserId(),
                null,
                IamAuditLog.ACTION_LOGOUT,
                "auth_session",
                token.getSessionId(),
                "用户主动登出",
                null);
    }

    /**
     * 刷新 access_token：refresh_token 一次性使用并轮换新令牌对。
     *
     * @param refreshToken 刷新令牌原文。
     * @return 新令牌对登录响应。
     */
    public LoginVO refresh(String refreshToken) {
        RefreshToken token = loadByHash(refreshToken);
        if (token == null) {
            throw new BusinessException(PlatformErrorCode.AUTH_TOKEN_INVALID, "refresh_token 无效");
        }
        if (RefreshToken.STATUS_USED.equals(token.getStatus()) || RefreshToken.STATUS_REVOKED.equals(token.getStatus())) {
            throw new BusinessException(PlatformErrorCode.AUTH_REFRESH_TOKEN_USED);
        }
        if (token.getExpiresAt().isBefore(LocalDateTime.now(ZoneOffset.UTC))) {
            throw new BusinessException(PlatformErrorCode.AUTH_TOKEN_EXPIRED, "refresh_token 已过期");
        }
        AuthSession session = sessionMapper.selectById(token.getSessionId());
        if (session == null || !AuthSession.STATUS_ACTIVE.equals(session.getStatus())) {
            throw new BusinessException(PlatformErrorCode.AUTH_TOKEN_INVALID, "会话已失效");
        }
        IamUser user = userMapper.selectById(token.getUserId());
        if (user == null || user.getDeletedAt() != null || !IamUser.STATUS_ACTIVE.equals(user.getStatus())) {
            throw new BusinessException(PlatformErrorCode.AUTH_TOKEN_INVALID, "用户不可用");
        }
        // 一次性消费旧令牌，再签发新令牌对。
        refreshTokenMapper.update(
                null,
                new LambdaUpdateWrapper<RefreshToken>()
                        .eq(RefreshToken::getId, token.getId())
                        .eq(RefreshToken::getStatus, RefreshToken.STATUS_ACTIVE)
                        .set(RefreshToken::getStatus, RefreshToken.STATUS_USED)
                        .set(RefreshToken::getUpdatedAt, LocalDateTime.now(ZoneOffset.UTC)));
        SessionService.IssuedTokens tokens = sessionService.issue(user, session.getAuthType(), session.getIp(), session.getUserAgent());
        sessionService.slide(session.getId());
        List<String> roleNames = roleMapper.selectByUserId(user.getId()).stream().map(IamRole::getName).toList();
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
     * 被动撤销（管理员强制下线）；需审计 token_revoke。
     *
     * @param sessionId 会话 ID。
     * @param reason 撤销原因。
     * @param operator 操作者上下文。
     */
    public void revoke(Long sessionId, String reason, AuthContext operator) {
        AuthSession session = sessionMapper.selectById(sessionId);
        if (session == null || !AuthSession.STATUS_ACTIVE.equals(session.getStatus())) {
            throw new BusinessException(PlatformErrorCode.RESOURCE_NOT_FOUND, "会话不存在或已失效");
        }
        if (!operator.isPlatformSuperAdmin()) {
            // 跨租户防护（W2-D-14 IDOR）：非平台超管只能撤销本租户用户的会话；
            // 归属不符时与不存在同样返回 RESOURCE_NOT_FOUND，不暴露其他租户会话存在性。
            IamUser target = userMapper.selectById(session.getUserId());
            if (target == null
                    || target.getDeletedAt() != null
                    || !Objects.equals(target.getTenantId(), operator.tenantId())) {
                throw new BusinessException(PlatformErrorCode.RESOURCE_NOT_FOUND, "会话不存在或已失效");
            }
        }
        sessionService.revokeSession(sessionId);
        permissionService.evict(session.getUserId());
        auditService.record(
                operator.userId(),
                operator.tenantId(),
                IamAuditLog.ACTION_TOKEN_REVOKE,
                "auth_session",
                sessionId,
                "管理员强制下线：" + (reason == null ? "未填写原因" : reason),
                null);
    }

    /**
     * 当前用户档案：基础信息、权限码、部署模式与后端推导的可见菜单树。
     *
     * @param userId 用户 ID。
     * @return 档案响应。
     */
    public ProfileVO profile(Long userId) {
        IamUser user = userMapper.selectById(userId);
        if (user == null || user.getDeletedAt() != null) {
            throw new BusinessException(PlatformErrorCode.AUTH_TOKEN_INVALID, "用户不存在或已删除");
        }
        List<IamRole> roles = roleMapper.selectByUserId(userId);
        Set<String> permissions = permissionService.effectivePermissions(user);
        List<MenuNodeVO> menus = menuService.visibleMenus(permissions);
        return new ProfileVO(
                String.valueOf(user.getId()),
                user.getUsername(),
                user.getDisplayName(),
                roles.stream().map(IamRole::getName).toList(),
                List.copyOf(permissions),
                appProperties.getDeploymentMode().name(),
                menus);
    }

    /**
     * 按摘要定位刷新令牌。
     *
     * @param refreshToken 令牌原文。
     * @return 令牌实体；不存在返回 null。
     */
    private RefreshToken loadByHash(String refreshToken) {
        return refreshTokenMapper.selectOne(
                new LambdaQueryWrapper<RefreshToken>()
                        .eq(RefreshToken::getTokenHash, SessionService.sha256Hex(refreshToken)));
    }
}
