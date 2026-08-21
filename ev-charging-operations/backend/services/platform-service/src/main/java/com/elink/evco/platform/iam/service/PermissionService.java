package com.elink.evco.platform.iam.service;

import com.elink.evco.platform.iam.entity.IamUser;
import com.elink.evco.web.security.PermissionCheckPort;
import java.util.Set;

/**
 * 权限读取服务：角色 → 权限并集经 Redis 缓存（TTL 与 access_token 对齐）， 角色变更或登出时显式驱逐；平台超级管理员返回全量启用权限码。
 *
 * <p>实现 starter 的 PermissionCheckPort 端口，供两档权限拦截器校验调用。
 */
public interface PermissionService extends PermissionCheckPort {

    /**
     * 读取用户有效权限码集合（实体入参重载）。
     *
     * @param user 目标用户（含 userType）。
     * @return 权限码集合；无权限时为空集合。
     */
    Set<String> effectivePermissions(IamUser user);

    /**
     * 驱逐用户权限缓存；角色替换、登出、撤销、停用时调用。
     *
     * @param userId 用户 ID。
     */
    void evict(Long userId);
}
