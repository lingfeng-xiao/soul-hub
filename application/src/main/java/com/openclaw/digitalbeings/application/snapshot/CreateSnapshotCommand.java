package com.openclaw.digitalbeings.application.snapshot;

import com.openclaw.digitalbeings.domain.core.SnapshotType;

public record CreateSnapshotCommand(
        String beingId,
        SnapshotType type,
        String summary,
        String actor
) {
}
