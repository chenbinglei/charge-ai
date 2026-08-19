package com.elink.evco.platform.iam.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * 被动撤销（管理员强制下线）请求；需审计 token_revoke。
 *
 * @param sessionId 会话 ID。
 * @param reason 撤销原因；写入审计摘要。
 */
public record RevokeRequest(@NotBlank(message = "sessionId 不能为空") String sessionId, String reason) {}
