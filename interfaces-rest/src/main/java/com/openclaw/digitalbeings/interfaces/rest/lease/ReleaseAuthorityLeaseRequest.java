package com.openclaw.digitalbeings.interfaces.rest.lease;

public record ReleaseAuthorityLeaseRequest(
        String beingId,
        String leaseId,
        String actor
) {
}
