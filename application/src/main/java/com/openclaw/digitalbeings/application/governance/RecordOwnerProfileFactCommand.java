package com.openclaw.digitalbeings.application.governance;

public record RecordOwnerProfileFactCommand(
        String beingId,
        String section,
        String key,
        String summary,
        String actor
) {
}
