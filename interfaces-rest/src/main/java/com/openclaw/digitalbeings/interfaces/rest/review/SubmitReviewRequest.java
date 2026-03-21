package com.openclaw.digitalbeings.interfaces.rest.review;

public record SubmitReviewRequest(
        String beingId,
        String reviewItemId,
        String actor
) {
}
