package com.lingfeng.sprite.runtime.event;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * ActionDispatchedEvent - Published when an action is dispatched for execution.
 *
 * This event marks the transition from decision to action execution.
 * It includes the action details and execution context.
 */
public final class ActionDispatchedEvent implements DomainEvent {

    private final String eventId;
    private final String cycleId;
    private final Instant occurredAt;
    private final Map<String, Object> metadata;
    private final String actionType;
    private final String actionContent;
    private final String target;
    private final String priority;

    private ActionDispatchedEvent(Builder builder) {
        this.eventId = builder.eventId;
        this.cycleId = builder.cycleId;
        this.occurredAt = builder.occurredAt;
        this.metadata = builder.metadata;
        this.actionType = builder.actionType;
        this.actionContent = builder.actionContent;
        this.target = builder.target;
        this.priority = builder.priority;
    }

    @Override
    public String getEventId() {
        return eventId;
    }

    @Override
    public String getEventType() {
        return "ActionDispatched";
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

    public String getActionType() {
        return actionType;
    }

    public String getActionContent() {
        return actionContent;
    }

    public String getTarget() {
        return target;
    }

    public String getPriority() {
        return priority;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static ActionDispatchedEvent create(String cycleId, String actionType, String actionContent) {
        return builder()
            .cycleId(cycleId)
            .actionType(actionType)
            .actionContent(actionContent)
            .build();
    }

    public static ActionDispatchedEvent create(String cycleId, String actionType, String actionContent, String target, String priority) {
        return builder()
            .cycleId(cycleId)
            .actionType(actionType)
            .actionContent(actionContent)
            .target(target)
            .priority(priority)
            .build();
    }

    public static class Builder extends DomainEvent.Builder<Builder> {
        private String actionType;
        private String actionContent;
        private String target;
        private String priority;

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

        public Builder actionType(String actionType) {
            this.actionType = actionType;
            return this;
        }

        public Builder actionContent(String actionContent) {
            this.actionContent = actionContent;
            return this;
        }

        public Builder target(String target) {
            this.target = target;
            return this;
        }

        public Builder priority(String priority) {
            this.priority = priority;
            return this;
        }

        public ActionDispatchedEvent build() {
            if (eventId == null) {
                eventId = UUID.randomUUID().toString();
            }
            if (cycleId == null) {
                throw new IllegalStateException("cycleId is required");
            }
            if (actionType == null) {
                throw new IllegalStateException("actionType is required");
            }
            return new ActionDispatchedEvent(this);
        }
    }

    @Override
    public String toString() {
        return "ActionDispatchedEvent{" +
            "eventId='" + eventId + '\'' +
            ", cycleId='" + cycleId + '\'' +
            ", occurredAt=" + occurredAt +
            ", actionType='" + actionType + '\'' +
            ", actionContent='" + actionContent + '\'' +
            ", target='" + target + '\'' +
            ", priority='" + priority + '\'' +
            '}';
    }
}
