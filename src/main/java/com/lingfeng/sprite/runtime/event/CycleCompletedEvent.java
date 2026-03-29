package com.lingfeng.sprite.runtime.event;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * CycleCompletedEvent - Published when a cognition cycle completes.
 *
 * This event marks the successful completion of a perception-cognition-action cycle.
 * It includes timing information and the outcome of the cycle.
 */
public final class CycleCompletedEvent implements DomainEvent {

    private final String eventId;
    private final String cycleId;
    private final Instant occurredAt;
    private final Map<String, Object> metadata;
    private final Duration cycleDuration;
    private final boolean success;
    private final String outcomeSummary;

    private CycleCompletedEvent(Builder builder) {
        this.eventId = builder.eventId;
        this.cycleId = builder.cycleId;
        this.occurredAt = builder.occurredAt;
        this.metadata = builder.metadata;
        this.cycleDuration = builder.cycleDuration;
        this.success = builder.success;
        this.outcomeSummary = builder.outcomeSummary;
    }

    @Override
    public String getEventId() {
        return eventId;
    }

    @Override
    public String getEventType() {
        return "CycleCompleted";
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

    public Duration getCycleDuration() {
        return cycleDuration;
    }

    public boolean isSuccess() {
        return success;
    }

    public String getOutcomeSummary() {
        return outcomeSummary;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static CycleCompletedEvent create(String cycleId, Duration cycleDuration, boolean success) {
        return builder()
            .cycleId(cycleId)
            .cycleDuration(cycleDuration)
            .success(success)
            .build();
    }

    public static CycleCompletedEvent create(String cycleId, Duration cycleDuration, boolean success, String outcomeSummary) {
        return builder()
            .cycleId(cycleId)
            .cycleDuration(cycleDuration)
            .success(success)
            .outcomeSummary(outcomeSummary)
            .build();
    }

    public static class Builder extends DomainEvent.Builder<Builder> {
        private Duration cycleDuration;
        private boolean success;
        private String outcomeSummary;

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

        public Builder cycleDuration(Duration cycleDuration) {
            this.cycleDuration = cycleDuration;
            return this;
        }

        public Builder success(boolean success) {
            this.success = success;
            return this;
        }

        public Builder outcomeSummary(String outcomeSummary) {
            this.outcomeSummary = outcomeSummary;
            return this;
        }

        public CycleCompletedEvent build() {
            if (eventId == null) {
                eventId = UUID.randomUUID().toString();
            }
            if (cycleId == null) {
                throw new IllegalStateException("cycleId is required");
            }
            return new CycleCompletedEvent(this);
        }
    }

    @Override
    public String toString() {
        return "CycleCompletedEvent{" +
            "eventId='" + eventId + '\'' +
            ", cycleId='" + cycleId + '\'' +
            ", occurredAt=" + occurredAt +
            ", cycleDuration=" + cycleDuration +
            ", success=" + success +
            ", outcomeSummary='" + outcomeSummary + '\'' +
            '}';
    }
}
