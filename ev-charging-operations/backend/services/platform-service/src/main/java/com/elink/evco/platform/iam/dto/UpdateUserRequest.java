package com.elink.evco.platform.iam.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 编辑管理用户资料请求；不通过本接口重置认证凭据。
 *
 * @param displayName 展示姓名；省略表示不变更。
 * @param version 乐观锁版本；冲突返回 VERSION_CONFLICT。
 */
public record UpdateUserRequest(
        @Size(max = 64, message = "展示姓名不超过 64 字符") String displayName,
        @NotNull(message = "version 不能为空") Integer version) {}
