package com.openclaw.digitalbeings.application.review;

public record SubmitReviewCommand(
        String beingId,
        String reviewItemId,
        String actor
) {

    public SubmitReviewCommand {
        beingId = requireText(beingId, "beingId");
        reviewItemId = requireText(reviewItemId, "reviewItemId");
        actor = requireText(actor, "actor");
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank.");
        }
        return value.trim();
    }
}
