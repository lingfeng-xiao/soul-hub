package com.openclaw.digitalbeings.interfaces.rest.review;

public record DraftReviewRequest(
        String beingId,
        String lane,
        String kind,
        String proposal,
        String actor
) {
}
