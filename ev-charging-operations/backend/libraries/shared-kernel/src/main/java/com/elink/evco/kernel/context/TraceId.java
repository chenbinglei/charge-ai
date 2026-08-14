package com.elink.evco.kernel.context;

import java.util.Objects;

/**
 * 在请求链路中传播的稳定、非敏感关联标识。
 */
public record TraceId(String value) {
    private static final int MAX_LENGTH = 128;

    public TraceId {
        value = Objects.requireNonNull(value, "traceId must not be null").trim();
        if (value.isEmpty() || value.length() > MAX_LENGTH) {
            throw new IllegalArgumentException("traceId must contain 1 to 128 characters");
        }
    }
}
