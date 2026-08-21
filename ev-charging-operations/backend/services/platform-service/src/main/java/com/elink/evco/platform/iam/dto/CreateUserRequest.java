package com.elink.evco.platform.iam.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;

/**
 * 新增管理用户请求；初始密码由服务端生成并在创建响应中一次性返回。
 *
 * @param username 登录名；租户范围内唯一，3-64 位字母数字下划线。
 * @param displayName 后台展示姓名。
 * @param roleIds 初始绑定角色 ID 集合；至少一个。
 * @param tenantId 所属租户 ID；平台管理员可省略。
 */
public record CreateUserRequest(
        @NotBlank(message = "登录名不能为空")
                @Pattern(regexp = "^[a-zA-Z0-9_]{3,64}$", message = "登录名为 3-64 位字母数字下划线")
                String username,
        @NotBlank(message = "展示姓名不能为空") @Size(max = 64, message = "展示姓名不超过 64 字符")
                String displayName,
        @NotEmpty(message = "初始角色不能为空") List<String> roleIds,
        String tenantId) {}
