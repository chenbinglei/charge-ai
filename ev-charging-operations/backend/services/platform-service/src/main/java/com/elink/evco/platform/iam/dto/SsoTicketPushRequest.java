package com.elink.evco.platform.iam.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;

/**
 * IOT 推送 SSO 一次性 ticket 请求（服务间接口，X-API-Key 鉴权）。
 *
 * @param ticket 一次性票据随机串（≥32 字符）；原文不写日志/审计。
 * @param iotUserId IOT 侧用户 ID；平台按 iam_user.iot_user_id 定位本地用户。
 * @param displayName IOT 侧展示名；仅用于审计。
 * @param expireAt 票据过期时间（UTC）。
 */
public record SsoTicketPushRequest(
        @NotBlank(message = "ticket 不能为空") @Size(min = 32, message = "ticket 长度必须不少于 32 字符")
                String ticket,
        @NotBlank(message = "iotUserId 不能为空") String iotUserId,
        String displayName,
        @NotNull(message = "expireAt 不能为空") Instant expireAt) {}
