package com.openclaw.digitalbeings.domain.governance;

import com.openclaw.digitalbeings.domain.core.UlidFactory;
import java.time.Instant;

public record OwnerProfileFact(
        String factId,
        String section,
        String key,
        String summary,
        Instant acceptedAt
) {

    public OwnerProfileFact {
        factId = requireText(factId, "factId");
        section = requireText(section, "section");
        key = requireText(key, "key");
        summary = requireText(summary, "summary");
        if (acceptedAt == null) {
            throw new IllegalArgumentException("acceptedAt must not be null.");
        }
    }

    public static OwnerProfileFact create(String section, String key, String summary, Instant acceptedAt) {
        return new OwnerProfileFact(UlidFactory.newUlid(), section, key, summary, acceptedAt);
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank.");
        }
        return value.trim();
    }
}
