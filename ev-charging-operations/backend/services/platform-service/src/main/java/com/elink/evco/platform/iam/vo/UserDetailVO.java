package com.elink.evco.platform.iam.vo;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 管理用户详情响应。
 *
 * @param id 用户 ID。
 * @param username 登录名。
 * @param displayName 展示姓名。
 * @param status 账户状态：active/locked/disabled。
 * @param version 乐观锁版本。
 * @param roles 绑定角色集合。
 * @param tenantId 所属租户 ID。
 * @param createdAt 创建时间（UTC）。
 * @param updatedAt 更新时间（UTC）。
 * @param initialPassword 初始密码；仅创建响应返回一次，首次登录须改密，其余场景为 null。
 */
public record UserDetailVO(
        String id,
        String username,
        String displayName,
        String status,
        Integer version,
        List<RoleOptionVO> roles,
        String tenantId,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        String initialPassword) {}
