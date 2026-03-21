package com.openclaw.digitalbeings.domain.runtime;

import com.openclaw.digitalbeings.domain.core.UlidFactory;
import java.time.Instant;

public record HostContract(
        String contractId,
        String hostType,
        String status,
        Instant registeredAt
) {

    public HostContract {
        contractId = requireText(contractId, "contractId");
        hostType = requireText(hostType, "hostType");
        status = requireText(status, "status");
        if (registeredAt == null) {
            throw new IllegalArgumentException("registeredAt must not be null.");
        }
    }

    public static HostContract active(String hostType, Instant registeredAt) {
        return new HostContract(UlidFactory.newUlid(), hostType, "ACTIVE", registeredAt);
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank.");
        }
        return value.trim();
    }
}
