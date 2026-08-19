package com.elink.evco.platform.iam.dto;

import jakarta.validation.constraints.NotNull;
import java.util.List;

/**
 * 以完整角色集合替换绑定请求；空集合表示解除全部绑定。
 *
 * @param roleIds 替换后的完整角色 ID 集合。
 * @param version 乐观锁版本；冲突返回 VERSION_CONFLICT。权限变更属敏感操作， 与其他写端点一致必须携带冲突信号。
 */
public record ReplaceUserRolesRequest(
        @NotNull(message = "roleIds 不能为空") List<String> roleIds,
        @NotNull(message = "version 不能为空") Integer version) {}
