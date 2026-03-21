package com.openclaw.digitalbeings.application.relationship;

public record CreateRelationshipEntityCommand(
        String beingId,
        String kind,
        String displayName,
        String actor
) {
}
