package com.openclaw.digitalbeings.interfaces.rest.lease;

public record AcquireAuthorityLeaseRequest(
        String beingId,
        String sessionId,
        String actor
) {
}
