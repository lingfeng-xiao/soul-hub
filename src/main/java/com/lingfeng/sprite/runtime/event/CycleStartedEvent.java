package com.lingfeng.sprite.runtime.event;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * CycleStartedEvent - Published when a new cognition cycle begins.
 *
 * This event marks the start of a complete perception-cognition-action cycle.
 * Subscribers can use this event to initialize per-cycle state or track cycle timing.
 */
public final class CycleStartedEvent implements DomainEvent {

    private final String eventId;
    private final String cycleId;
    private final Instant occurredAt;
    private final Map<String, Object> metadata;
    private final String triggerSource;
    private final int cycleCount;

    private CycleStartedEvent(Builder builder) {
        this.eventId = builder.eventId;
        this.cycleId = builder.cycleId;
        this.occurredAt = builder.occurredAt;
        this.metadata = builder.metadata;
        this.triggerSource = builder.triggerSource;
        this.cycleCount = builder.cycleCount;
    }

    @Override
    public String getEventId() {
        return eventId;
    }

    @Override
    public String getEventType() {
        return "CycleStarted";
    }

    @Override
    public String getCycleId() {
        return cycleId;
    }

    @Override
    public Instant getOccurredAt() {
        return occurredAt;
    }

    @Override
    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public String getTriggerSource() {
        return triggerSource;
    }

    public int getCycleCount() {
        return cycleCount;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static CycleStartedEvent create(String cycleId, int cycleCount) {
        return builder()
            .cycleId(cycleId)
            .cycleCount(cycleCount)
            .build();
    }

    public static CycleStartedEvent create(String cycleId, int cycleCount, String triggerSource) {
        return builder()
            .cycleId(cycleId)
            .cycleCount(cycleCount)
            .triggerSource(triggerSource)
            .build();
    }

    public static class Builder extends DomainEvent.Builder<Builder> {
        private String triggerSource;
        private int cycleCount;

        @Override
        public Builder eventId(String eventId) {
            super.eventId(eventId);
            return this;
        }

        @Override
        public Builder eventType(String eventType) {
            super.eventType(eventType);
            return this;
        }

        @Override
        public Builder cycleId(String cycleId) {
            super.cycleId(cycleId);
            return this;
        }

        @Override
        public Builder occurredAt(Instant occurredAt) {
            super.occurredAt(occurredAt);
            return this;
        }

        @Override
        public Builder metadata(Map<String, Object> metadata) {
            super.metadata(metadata);
            return this;
        }

        @Override
        public Builder putMetadata(String key, Object value) {
            super.putMetadata(key, value);
            return this;
        }

        public Builder triggerSource(String triggerSource) {
            this.triggerSource = triggerSource;
            return this;
        }

        public Builder cycleCount(int cycleCount) {
            this.cycleCount = cycleCount;
            return this;
        }

        public CycleStartedEvent build() {
            if (eventId == null) {
                eventId = UUID.randomUUID().toString();
            }
            if (cycleId == null) {
                throw new IllegalStateException("cycleId is required");
            }
            return new CycleStartedEvent(this);
        }
    }

    @Override
    public String toString() {
        return "CycleStartedEvent{" +
            "eventId='" + eventId + '\'' +
            ", cycleId='" + cycleId + '\'' +
            ", occurredAt=" + occurredAt +
            ", triggerSource='" + triggerSource + '\'' +
            ", cycleCount=" + cycleCount +
            '}';
    }
}
