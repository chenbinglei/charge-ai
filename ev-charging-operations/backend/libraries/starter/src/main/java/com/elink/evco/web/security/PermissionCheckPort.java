package com.elink.evco.web.security;

import java.util.Set;

/**
 * 有效权限读取端口：由具体服务实现，供两档权限拦截器校验 resource:read/write。
 *
 * <p>拦截器不感知权限存储（角色并集、缓存策略等）；实现方负责缓存与驱逐。
 */
public interface PermissionCheckPort {

    /**
     * 读取用户有效权限码集合。
     *
     * @param userId 用户 ID。
     * @param userType 用户类型：PLATFORM_SUPER/TENANT_ADMIN/NORMAL。
     * @return 权限码集合；无权限时为空集合。
     */
    Set<String> effectivePermissions(Long userId, String userType);
}
