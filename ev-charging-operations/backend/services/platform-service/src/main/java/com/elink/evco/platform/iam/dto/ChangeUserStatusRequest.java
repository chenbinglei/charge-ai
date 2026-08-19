package com.elink.evco.platform.iam.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * 用户状态迁移请求；迁移以 IAM 状态机为准。
 *
 * @param status 目标状态：active/locked/disabled。
 */
public record ChangeUserStatusRequest(
        @NotBlank(message = "status 不能为空")
                @Pattern(
                        regexp = "active|locked|disabled",
                        message = "status 仅允许 active/locked/disabled")
                String status) {}
