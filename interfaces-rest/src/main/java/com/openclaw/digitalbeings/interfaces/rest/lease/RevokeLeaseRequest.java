package com.openclaw.digitalbeings.interfaces.rest.lease;

public record RevokeLeaseRequest(
        String beingId,
        String actor
) {
}
