package com.openclaw.digitalbeings.application.event;

import com.openclaw.digitalbeings.domain.being.Being;
import com.openclaw.digitalbeings.application.support.BeingStore;
import com.openclaw.digitalbeings.domain.events.DomainEventRecord;
import com.openclaw.digitalbeings.domain.core.DomainEventType;
import org.springframework.stereotype.Service;
import java.time.Instant;
import java.util.List;

@Service
public class EventQueryService {
    private final BeingStore beingStore;

    public EventQueryService(BeingStore beingStore) {
        this.beingStore = beingStore;
    }

    public List<DomainEventRecord> queryEvents(String beingId, DomainEventType eventType, Instant from, Instant to, int limit) {
        Being being = beingStore.requireById(beingId);
        return being.domainEvents().stream()
            .filter(e -> eventType == null || e.eventType() == eventType)
            .filter(e -> from == null || !e.occurredAt().isBefore(from))
            .filter(e -> to == null || !e.occurredAt().isAfter(to))
            .limit(limit > 0 ? limit : 100)
            .toList();
    }
}
