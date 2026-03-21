package com.openclaw.digitalbeings.interfaces.rest.being;

public record AddIdentityFacetRequest(
        String kind,
        String summary,
        String actor
) {
}
