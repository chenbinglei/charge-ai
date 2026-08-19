package com.elink.evco.platform.iam.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * 平台管理端登录请求；图形验证码一次性校验，连续 5 次失败锁定 30 分钟。
 *
 * @param username 登录名。
 * @param password 密码原文；仅用于 BCrypt 比对，不落日志。
 * @param captchaId 图形验证码标识。
 * @param captchaCode 图形验证码内容；大小写不敏感。
 * @param tenantId 租户 ID；平台管理员可省略。
 */
public record LoginRequest(
        @NotBlank(message = "登录名不能为空") String username,
        @NotBlank(message = "密码不能为空") String password,
        @NotBlank(message = "验证码标识不能为空") String captchaId,
        @NotBlank(message = "验证码不能为空") String captchaCode,
        String tenantId) {}
