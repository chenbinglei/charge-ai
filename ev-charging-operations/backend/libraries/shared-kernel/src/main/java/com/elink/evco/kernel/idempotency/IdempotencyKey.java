package com.elink.evco.kernel.idempotency;

import java.util.Objects;

/**
 * 调用方提供的单一命令防重键，界定重复检测范围。
 */
public record IdempotencyKey(String value) {
    private static final int MAX_LENGTH = 128;

    public IdempotencyKey {
        value = Objects.requireNonNull(value, "idempotencyKey must not be null").trim();
        if (value.isEmpty() || value.length() > MAX_LENGTH) {
            throw new IllegalArgumentException("idempotencyKey must contain 1 to 128 characters");
        }
    }
}
