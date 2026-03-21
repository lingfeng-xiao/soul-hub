package com.openclaw.digitalbeings.interfaces.rest.snapshot;

public record CreateSnapshotRequest(
        String beingId,
        String type,
        String summary,
        String actor
) {
}
