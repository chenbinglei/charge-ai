package com.elink.evco.web.security;

/**
 * 已认证请求的上下文快照；内容来自 JWT 声明，签发后不可变。
 *
 * @param userId 用户 ID（iam_user.id）。
 * @param sessionId 会话 ID（auth_session.id）。
 * @param username 登录名。
 * @param displayName 展示姓名。
 * @param tenantId 所属租户 ID。
 * @param userType 用户类型：PLATFORM_SUPER/TENANT_ADMIN/NORMAL。
 * @param authType 认证方式：PASSWORD/SSO。
 */
public record AuthContext(
        Long userId,
        Long sessionId,
        String username,
        String displayName,
        Long tenantId,
        String userType,
        String authType) {

    /** 平台超级管理员用户类型；拥有全部权限，鉴权短路放行。 */
    public static final String USER_TYPE_PLATFORM_SUPER = "PLATFORM_SUPER";

    /**
     * 判断当前主体是否平台超级管理员。
     *
     * @return true 表示超管，权限校验短路放行。
     */
    public boolean isPlatformSuperAdmin() {
        return USER_TYPE_PLATFORM_SUPER.equals(userType);
    }
}
