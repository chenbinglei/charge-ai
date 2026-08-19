package com.elink.evco.platform.iam.vo;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 管理用户列表项；不返回密码、令牌或敏感认证材料。
 *
 * @param id 用户 ID。
 * @param username 登录名。
 * @param displayName 展示姓名。
 * @param status 账户状态：active/locked/disabled。
 * @param roleNames 角色名称集合。
 * @param tenantId 所属租户 ID。
 * @param createdAt 创建时间（UTC）。
 */
public record UserListVO(
        String id,
        String username,
        String displayName,
        String status,
        List<String> roleNames,
        String tenantId,
        LocalDateTime createdAt) {}
