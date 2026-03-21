package com.openclaw.digitalbeings.interfaces.rest.governance;

public record RecordOwnerProfileFactRequest(
        String beingId,
        String section,
        String key,
        String summary,
        String actor
) {
}
