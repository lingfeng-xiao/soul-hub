package com.lingfeng.sprite.runtime.event;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * DecisionSelectedEvent - Published when the decision engine selects an action.
 *
 * This event captures the decision-making outcome, including the selected action,
 * the reasoning context, and the confidence level.
 */
public final class DecisionSelectedEvent implements DomainEvent {

    private final String eventId;
    private final String cycleId;
    private final Instant occurredAt;
    private final Map<String, Object> metadata;
    private final String decisionType;
    private final String selectedAction;
    private final float confidence;
    private final String reasoning;

    private DecisionSelectedEvent(Builder builder) {
        this.eventId = builder.eventId;
        this.cycleId = builder.cycleId;
        this.occurredAt = builder.occurredAt;
        this.metadata = builder.metadata;
        this.decisionType = builder.decisionType;
        this.selectedAction = builder.selectedAction;
        this.confidence = builder.confidence;
        this.reasoning = builder.reasoning;
    }

    @Override
    public String getEventId() {
        return eventId;
    }

    @Override
    public String getEventType() {
        return "DecisionSelected";
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

    public String getDecisionType() {
        return decisionType;
    }

    public String getSelectedAction() {
        return selectedAction;
    }

    public float getConfidence() {
        return confidence;
    }

    public String getReasoning() {
        return reasoning;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static DecisionSelectedEvent create(String cycleId, String selectedAction, float confidence) {
        return builder()
            .cycleId(cycleId)
            .selectedAction(selectedAction)
            .confidence(confidence)
            .build();
    }

    public static DecisionSelectedEvent create(String cycleId, String decisionType, String selectedAction, float confidence, String reasoning) {
        return builder()
            .cycleId(cycleId)
            .decisionType(decisionType)
            .selectedAction(selectedAction)
            .confidence(confidence)
            .reasoning(reasoning)
            .build();
    }

    public static class Builder extends DomainEvent.Builder<Builder> {
        private String decisionType;
        private String selectedAction;
        private float confidence;
        private String reasoning;

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

        public Builder decisionType(String decisionType) {
            this.decisionType = decisionType;
            return this;
        }

        public Builder selectedAction(String selectedAction) {
            this.selectedAction = selectedAction;
            return this;
        }

        public Builder confidence(float confidence) {
            this.confidence = confidence;
            return this;
        }

        public Builder reasoning(String reasoning) {
            this.reasoning = reasoning;
            return this;
        }

        public DecisionSelectedEvent build() {
            if (eventId == null) {
                eventId = UUID.randomUUID().toString();
            }
            if (cycleId == null) {
                throw new IllegalStateException("cycleId is required");
            }
            if (selectedAction == null) {
                throw new IllegalStateException("selectedAction is required");
            }
            return new DecisionSelectedEvent(this);
        }
    }

    @Override
    public String toString() {
        return "DecisionSelectedEvent{" +
            "eventId='" + eventId + '\'' +
            ", cycleId='" + cycleId + '\'' +
            ", occurredAt=" + occurredAt +
            ", decisionType='" + decisionType + '\'' +
            ", selectedAction='" + selectedAction + '\'' +
            ", confidence=" + confidence +
            ", reasoning='" + reasoning + '\'' +
            '}';
    }
}
