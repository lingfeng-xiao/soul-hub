package com.openclaw.digitalbeings.interfaces.rest.review;

public record RebuildCanonicalProjectionRequest(
        String beingId,
        String actor
) {
}
