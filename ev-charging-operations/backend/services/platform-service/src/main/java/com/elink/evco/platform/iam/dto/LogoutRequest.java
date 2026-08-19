package com.elink.evco.platform.iam.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * 主动登出请求；撤销会话与关联刷新令牌。
 *
 * @param refreshToken 刷新令牌原文。
 */
public record LogoutRequest(@NotBlank(message = "refreshToken 不能为空") String refreshToken) {}
