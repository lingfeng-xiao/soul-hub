package com.openclaw.digitalbeings.infrastructure.neo4j.persistence.entity;

import com.openclaw.digitalbeings.domain.core.DomainEventType;
import java.time.Instant;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Property;

@Node("DomainEvent")
public class DomainEventNode {

    @Id
    private String eventId;

    @Property("eventType")
    private DomainEventType eventType;

    @Property("occurredAt")
    private Instant occurredAt;

    @Property("actor")
    private String actor;

    @Property("summary")
    private String summary;

    public DomainEventNode() {
    }

    public DomainEventNode(String eventId, DomainEventType eventType, Instant occurredAt, String actor, String summary) {
        this.eventId = eventId;
        this.eventType = eventType;
        this.occurredAt = occurredAt;
        this.actor = actor;
        this.summary = summary;
    }

    public String getEventId() {
        return eventId;
    }

    public void setEventId(String eventId) {
        this.eventId = eventId;
    }

    public DomainEventType getEventType() {
        return eventType;
    }

    public void setEventType(DomainEventType eventType) {
        this.eventType = eventType;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }

    public void setOccurredAt(Instant occurredAt) {
        this.occurredAt = occurredAt;
    }

    public String getActor() {
        return actor;
    }

    public void setActor(String actor) {
        this.actor = actor;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }
}
