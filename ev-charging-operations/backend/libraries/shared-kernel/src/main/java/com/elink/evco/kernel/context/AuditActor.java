package com.elink.evco.kernel.context;

import java.util.Objects;

/**
 * Non-sensitive reference to the actor responsible for an auditable action.
 */
public record AuditActor(String type, String reference) {
    public AuditActor {
        type = requireValue(type, "type");
        reference = requireValue(reference, "reference");
    }

    private static String requireValue(String value, String fieldName) {
        var normalized = Objects.requireNonNull(value, fieldName + " must not be null").trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return normalized;
    }
}
