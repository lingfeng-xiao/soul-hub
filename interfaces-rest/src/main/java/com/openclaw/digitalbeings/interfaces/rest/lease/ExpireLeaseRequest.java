package com.openclaw.digitalbeings.interfaces.rest.lease;

public record ExpireLeaseRequest(
        String beingId,
        String actor
) {
}
