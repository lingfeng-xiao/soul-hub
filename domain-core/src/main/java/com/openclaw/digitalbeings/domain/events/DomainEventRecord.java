package com.openclaw.digitalbeings.domain.events;

import com.openclaw.digitalbeings.domain.core.DomainEventType;
import com.openclaw.digitalbeings.domain.core.UlidFactory;
import java.time.Instant;

public record DomainEventRecord(
        String eventId,
        DomainEventType eventType,
        Instant occurredAt,
        String actor,
        String summary
) {

    public DomainEventRecord {
        eventId = requireText(eventId, "eventId");
        if (eventType == null) {
            throw new IllegalArgumentException("eventType must not be null.");
        }
        if (occurredAt == null) {
            throw new IllegalArgumentException("occurredAt must not be null.");
        }
        actor = requireText(actor, "actor");
        summary = requireText(summary, "summary");
    }

    public static DomainEventRecord create(DomainEventType eventType, String actor, Instant occurredAt, String summary) {
        return new DomainEventRecord(UlidFactory.newUlid(), eventType, occurredAt, actor, summary);
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank.");
        }
        return value.trim();
    }
}
