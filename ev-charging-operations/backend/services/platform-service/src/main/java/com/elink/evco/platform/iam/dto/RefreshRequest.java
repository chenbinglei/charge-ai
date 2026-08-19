package com.elink.evco.platform.iam.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * 刷新 access_token 请求；refresh_token 一次性使用后轮换。
 *
 * @param refreshToken 刷新令牌原文。
 */
public record RefreshRequest(@NotBlank(message = "refreshToken 不能为空") String refreshToken) {}
