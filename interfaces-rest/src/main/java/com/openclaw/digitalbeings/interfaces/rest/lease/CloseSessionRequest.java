package com.openclaw.digitalbeings.interfaces.rest.lease;

public record CloseSessionRequest(
        String beingId,
        String actor
) {
}
