package com.openclaw.digitalbeings.application.lease;

public record AcquireAuthorityLeaseCommand(
        String beingId,
        String sessionId,
        String actor
) {

    public AcquireAuthorityLeaseCommand {
        beingId = requireText(beingId, "beingId");
        sessionId = requireText(sessionId, "sessionId");
        actor = requireText(actor, "actor");
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank.");
        }
        return value.trim();
    }
}
