package com.lingfeng.sprite.runtime.event;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * DomainEvent - Base interface for all domain events in the Sprite runtime.
 *
 * All events share common fields for tracking and correlation:
 * - eventId: Unique identifier for this event instance
 * - eventType: The type/class of event
 * - cycleId: The cognition cycle this event belongs to
 * - occurredAt: When the event occurred
 * - metadata: Additional context-specific data
 */
public interface DomainEvent {

    /**
     * Get the unique identifier for this event.
     */
    String getEventId();

    /**
     * Get the simple name of the event type.
     */
    String getEventType();

    /**
     * Get the cognition cycle ID this event belongs to.
     */
    String getCycleId();

    /**
     * Get the timestamp when this event occurred.
     */
    Instant getOccurredAt();

    /**
     * Get additional metadata associated with this event.
     */
    Map<String, Object> getMetadata();

    /**
     * Builder-style interface for creating domain events.
     */
    abstract class Builder<T extends Builder<T>> {
        protected String eventId = UUID.randomUUID().toString();
        protected String eventType;
        protected String cycleId;
        protected Instant occurredAt = Instant.now();
        protected Map<String, Object> metadata = Map.of();

        @SuppressWarnings("unchecked")
        protected T self() {
            return (T) this;
        }

        public T eventId(String eventId) {
            this.eventId = eventId;
            return self();
        }

        public T eventType(String eventType) {
            this.eventType = eventType;
            return self();
        }

        public T cycleId(String cycleId) {
            this.cycleId = cycleId;
            return self();
        }

        public T occurredAt(Instant occurredAt) {
            this.occurredAt = occurredAt;
            return self();
        }

        public T metadata(Map<String, Object> metadata) {
            this.metadata = metadata;
            return self();
        }

        public T putMetadata(String key, Object value) {
            this.metadata = new java.util.HashMap<>(this.metadata);
            this.metadata.put(key, value);
            return self();
        }
    }
}
