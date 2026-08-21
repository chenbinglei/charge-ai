package com.elink.evco.kernel.api;

import java.time.Instant;
import java.util.Objects;

/**
 * 所有版本化 HTTP API 共用的稳定响应外观。
 *
 * @param <T> 已登记的响应数据类型；不得用未声明的动态对象替代。
 * @param code 稳定结果码。
 * @param message 面向调用方的中文结果说明。
 * @param data 已定义的响应数据；失败时允许为 {@code null}。
 * @param traceId 用于定位日志、审计和告警的关联标识。
 * @param timestamp 服务端生成的 UTC 响应时间。
 */
public record ApiResponse<T>(
        String code, String message, T data, String traceId, Instant timestamp) {
    /**
     * 构造成功响应，避免各服务重新拼接不一致的外观字段。
     *
     * @param data 已定义的响应数据。
     * @param traceId 当前请求链路标识。
     * @param <T> 响应数据类型。
     * @return 固定使用 {@code SUCCESS} 结果码的响应。
     */
    public static <T> ApiResponse<T> success(T data, String traceId) {
        return new ApiResponse<>("SUCCESS", "操作成功", data, traceId, Instant.now());
    }

    /**
     * 构造失败响应；异常详情只能进入受控日志，不能在响应中泄露。
     *
     * @param code 已登记的稳定失败码。
     * @param message 面向调用方的中文说明。
     * @param traceId 当前请求链路标识。
     * @return 不含业务数据的失败响应。
     */
    public static ApiResponse<Void> failure(String code, String message, String traceId) {
        return new ApiResponse<>(code, message, null, traceId, Instant.now());
    }

    /** 校验响应中不能出现空白的结果码、提示或追踪标识。 */
    public ApiResponse {
        code = requireText(code, "code");
        message = requireText(message, "message");
        traceId = requireText(traceId, "traceId");
        timestamp = Objects.requireNonNull(timestamp, "timestamp 不能为空");
    }

    /**
     * 统一校验面向调用方的基础文本字段。
     *
     * @param value 待校验文本。
     * @param fieldName 技术字段名称。
     * @return 去除首尾空白后的文本。
     */
    private static String requireText(String value, String fieldName) {
        var normalized = Objects.requireNonNull(value, fieldName + " 不能为空").trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException(fieldName + " 不能为空白");
        }
        return normalized;
    }
}
