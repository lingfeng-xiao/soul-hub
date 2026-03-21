package com.openclaw.digitalbeings.interfaces.rest.relationship;

public record CreateRelationshipEntityRequest(
        String beingId,
        String kind,
        String displayName,
        String actor
) {
}
