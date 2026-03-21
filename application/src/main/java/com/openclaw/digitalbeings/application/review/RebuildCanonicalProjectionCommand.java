package com.openclaw.digitalbeings.application.review;

public record RebuildCanonicalProjectionCommand(
        String beingId,
        String actor
) {

    public RebuildCanonicalProjectionCommand {
        beingId = requireText(beingId, "beingId");
        actor = requireText(actor, "actor");
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank.");
        }
        return value.trim();
    }
}
