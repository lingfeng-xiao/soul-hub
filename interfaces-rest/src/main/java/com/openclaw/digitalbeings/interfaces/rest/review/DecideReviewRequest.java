package com.openclaw.digitalbeings.interfaces.rest.review;

public record DecideReviewRequest(
        String beingId,
        String reviewItemId,
        String decision,
        String actor
) {
}
