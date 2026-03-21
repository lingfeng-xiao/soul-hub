package com.openclaw.digitalbeings.application.review;

public record DecideReviewCommand(
        String beingId,
        String reviewItemId,
        ReviewDecision decision,
        String actor
) {

    public DecideReviewCommand {
        beingId = requireText(beingId, "beingId");
        reviewItemId = requireText(reviewItemId, "reviewItemId");
        if (decision == null) {
            throw new IllegalArgumentException("decision must not be null.");
        }
        actor = requireText(actor, "actor");
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank.");
        }
        return value.trim();
    }
}
