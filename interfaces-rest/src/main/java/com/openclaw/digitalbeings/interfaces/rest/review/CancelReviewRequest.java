package com.openclaw.digitalbeings.interfaces.rest.review;

public record CancelReviewRequest(
        String beingId,
        String actor
) {
}
