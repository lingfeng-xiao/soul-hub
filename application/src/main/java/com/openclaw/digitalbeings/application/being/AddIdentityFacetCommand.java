package com.openclaw.digitalbeings.application.being;

public record AddIdentityFacetCommand(
        String beingId,
        String kind,
        String summary,
        String actor
) {
}
