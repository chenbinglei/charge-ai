package com.elink.evco.kernel.context;

import java.util.Objects;

/**
 * Stable, non-sensitive correlation identifier propagated across a request chain.
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
