package com.openclaw.digitalbeings.infrastructure.neo4j.persistence.mapper;

import com.openclaw.digitalbeings.domain.events.DomainEventRecord;
import com.openclaw.digitalbeings.infrastructure.neo4j.persistence.entity.DomainEventNode;
import java.util.List;

final class AuditSliceMapper {

    private AuditSliceMapper() {
    }

    static List<DomainEventNode> toEventNodes(List<DomainEventRecord> events) {
        return events.stream()
                .map(eventRecord -> new DomainEventNode(
                        eventRecord.eventId(),
                        eventRecord.eventType(),
                        eventRecord.occurredAt(),
                        eventRecord.actor(),
                        eventRecord.summary()
                ))
                .toList();
    }

    static List<DomainEventRecord> toDomainEvents(List<DomainEventNode> nodes) {
        return nodes.stream()
                .map(node -> new DomainEventRecord(
                        node.getEventId(),
                        node.getEventType(),
                        node.getOccurredAt(),
                        node.getActor(),
                        node.getSummary()
                ))
                .toList();
    }
}
