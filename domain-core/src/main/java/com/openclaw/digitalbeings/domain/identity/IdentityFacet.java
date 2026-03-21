package com.openclaw.digitalbeings.domain.identity;

import com.openclaw.digitalbeings.domain.core.UlidFactory;
import java.time.Instant;

public record IdentityFacet(
        String facetId,
        String kind,
        String summary,
        Instant recordedAt
) {

    public IdentityFacet {
        facetId = requireText(facetId, "facetId");
        kind = requireText(kind, "kind");
        summary = requireText(summary, "summary");
        if (recordedAt == null) {
            throw new IllegalArgumentException("recordedAt must not be null.");
        }
    }

    public static IdentityFacet create(String kind, String summary, Instant recordedAt) {
        return new IdentityFacet(UlidFactory.newUlid(), kind, summary, recordedAt);
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank.");
        }
        return value.trim();
    }
}
