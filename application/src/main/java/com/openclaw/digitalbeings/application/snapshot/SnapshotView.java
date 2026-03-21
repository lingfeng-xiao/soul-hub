package com.openclaw.digitalbeings.application.snapshot;

import com.openclaw.digitalbeings.domain.snapshot.ContinuitySnapshot;
import java.time.Instant;

public record SnapshotView(
        String beingId,
        String snapshotId,
        String type,
        String summary,
        Instant createdAt
) {

    public static SnapshotView from(String beingId, ContinuitySnapshot snapshot) {
        return new SnapshotView(
                beingId,
                snapshot.snapshotId(),
                snapshot.type().name(),
                snapshot.summary(),
                snapshot.createdAt()
        );
    }
}
