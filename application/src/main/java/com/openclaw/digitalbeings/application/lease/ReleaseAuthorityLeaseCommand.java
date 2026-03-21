package com.openclaw.digitalbeings.application.lease;

public record ReleaseAuthorityLeaseCommand(
        String beingId,
        String leaseId,
        String actor
) {

    public ReleaseAuthorityLeaseCommand {
        beingId = requireText(beingId, "beingId");
        leaseId = requireText(leaseId, "leaseId");
        actor = requireText(actor, "actor");
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank.");
        }
        return value.trim();
    }
}
