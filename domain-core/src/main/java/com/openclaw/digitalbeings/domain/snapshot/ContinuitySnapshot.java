package com.openclaw.digitalbeings.domain.snapshot;

import com.openclaw.digitalbeings.domain.core.SnapshotType;
import com.openclaw.digitalbeings.domain.core.UlidFactory;
import java.time.Instant;

public record ContinuitySnapshot(
        String snapshotId,
        SnapshotType type,
        String summary,
        Instant createdAt
) {

    public ContinuitySnapshot {
        snapshotId = requireText(snapshotId, "snapshotId");
        if (type == null) {
            throw new IllegalArgumentException("type must not be null.");
        }
        summary = requireText(summary, "summary");
        if (createdAt == null) {
            throw new IllegalArgumentException("createdAt must not be null.");
        }
    }

    public static ContinuitySnapshot create(SnapshotType type, String summary, Instant createdAt) {
        return new ContinuitySnapshot(UlidFactory.newUlid(), type, summary, createdAt);
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank.");
        }
        return value.trim();
    }
}
