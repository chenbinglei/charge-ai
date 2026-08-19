package com.elink.evco.platform.iam.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * SSO 免登录请求；ticket 经 Redis GETDEL 原子消费，防重放。
 *
 * @param ticket 跳转 URL 携带的一次性票据。
 */
public record SsoLoginRequest(@NotBlank(message = "ticket 不能为空") String ticket) {}
