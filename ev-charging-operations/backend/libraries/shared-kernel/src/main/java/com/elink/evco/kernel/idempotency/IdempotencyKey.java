package com.elink.evco.kernel.idempotency;

import java.util.Objects;

/**
 * Caller-supplied key for a single command's duplicate-detection boundary.
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
