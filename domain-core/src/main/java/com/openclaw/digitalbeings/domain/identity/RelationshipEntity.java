package com.openclaw.digitalbeings.domain.identity;

import com.openclaw.digitalbeings.domain.core.UlidFactory;
import java.time.Instant;

public record RelationshipEntity(
        String entityId,
        String kind,
        String displayName,
        Instant recordedAt
) {

    public RelationshipEntity {
        entityId = requireText(entityId, "entityId");
        kind = requireText(kind, "kind");
        displayName = requireText(displayName, "displayName");
        if (recordedAt == null) {
            throw new IllegalArgumentException("recordedAt must not be null.");
        }
    }

    public static RelationshipEntity create(String kind, String displayName, Instant recordedAt) {
        return new RelationshipEntity(UlidFactory.newUlid(), kind, displayName, recordedAt);
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank.");
        }
        return value.trim();
    }
}
