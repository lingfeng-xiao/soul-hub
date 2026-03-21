package com.openclaw.digitalbeings.application.review;

public record CancelReviewCommand(
        String beingId,
        String reviewItemId,
        String actor
) {
}
