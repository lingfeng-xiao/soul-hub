package com.openclaw.digitalbeings.domain.governance;

import com.openclaw.digitalbeings.domain.core.UlidFactory;
import java.time.Instant;

public record ManagedAgentSpec(
        String managedAgentId,
        String role,
        String status,
        Instant createdAt
) {

    public ManagedAgentSpec {
        managedAgentId = requireText(managedAgentId, "managedAgentId");
        role = requireText(role, "role");
        status = requireText(status, "status");
        if (createdAt == null) {
            throw new IllegalArgumentException("createdAt must not be null.");
        }
    }

    public static ManagedAgentSpec create(String role, String status, Instant createdAt) {
        return new ManagedAgentSpec(UlidFactory.newUlid(), role, status, createdAt);
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank.");
        }
        return value.trim();
    }
}
