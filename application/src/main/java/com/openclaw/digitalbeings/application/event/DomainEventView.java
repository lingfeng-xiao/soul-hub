package com.openclaw.digitalbeings.application.event;

import com.openclaw.digitalbeings.domain.events.DomainEventRecord;
import com.openclaw.digitalbeings.domain.core.DomainEventType;
import java.time.Instant;

public record DomainEventView(
    String eventId,
    DomainEventType eventType,
    String actor,
    Instant occurredAt,
    String summary
) {
    public static DomainEventView from(DomainEventRecord record) {
        return new DomainEventView(
            record.eventId(),
            record.eventType(),
            record.actor(),
            record.occurredAt(),
            record.summary()
        );
    }
}
