package com.elink.evco.kernel.context;

import java.util.Objects;

/**
 * 在请求链路中传播的稳定、非敏感关联标识。
 *
 * @param value 调用链唯一标识；只可保存技术追踪值，不能承载用户、订单或凭据原文。
 */
public record TraceId(String value) {
    /** 单个追踪标识允许的最大字符数，避免日志和响应被异常输入放大。 */
    private static final int MAX_LENGTH = 128;

    /**
     * 规范化并校验追踪标识，保证所有入口可安全写入日志和响应头。
     */
    public TraceId {
        value = Objects.requireNonNull(value, "traceId 不能为空").trim();
        if (value.isEmpty() || value.length() > MAX_LENGTH) {
            throw new IllegalArgumentException("traceId 长度必须为 1 到 128 个字符");
        }
    }
}
