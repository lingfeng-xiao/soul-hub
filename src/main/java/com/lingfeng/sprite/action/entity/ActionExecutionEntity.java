package com.lingfeng.sprite.action.entity;

import com.lingfeng.sprite.action.ActionStatus;

import jakarta.persistence.*;
import java.time.Instant;

/**
 * ActionExecutionEntity - JPA entity for persisting action execution records
 *
 * This entity tracks the lifecycle of action executions and supports
 * the action state machine: PENDING -> RUNNING -> SUCCEEDED/FAILED/COMPENSATED
 */
@Entity
@Table(name = "action_executions", indexes = {
    @Index(name = "idx_action_task_id", columnList = "taskId"),
    @Index(name = "idx_action_status", columnList = "status"),
    @Index(name = "idx_action_idempotency_key", columnList = "idempotencyKey"),
    @Index(name = "idx_action_cycle_id", columnList = "cycleId")
})
public class ActionExecutionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false)
    private String taskId;

    @Column(nullable = false)
    private String executionId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ActionStatus status;

    @Column
    private String actionType;

    @Column
    private String target;

    @Column(columnDefinition = "TEXT")
    private String parameters;

    @Column(columnDefinition = "TEXT")
    private String requestPayload;

    @Column(columnDefinition = "TEXT")
    private String responsePayload;

    @Column
    private String errorMessage;

    @Column
    private int retryCount;

    @Column
    private String idempotencyKey;

    @Column
    private String cycleId;

    @Column
    private String planId;

    @Column
    private Instant createdAt;

    @Column
    private Instant startedAt;

    @Column
    private Instant finishedAt;

    @Column
    private Instant updatedAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
        updatedAt = Instant.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }

    // Default constructor required by JPA
    public ActionExecutionEntity() {
    }

    // Builder-style constructor
    public ActionExecutionEntity(Builder builder) {
        this.id = builder.id;
        this.taskId = builder.taskId;
        this.executionId = builder.executionId;
        this.status = builder.status;
        this.actionType = builder.actionType;
        this.target = builder.target;
        this.parameters = builder.parameters;
        this.requestPayload = builder.requestPayload;
        this.responsePayload = builder.responsePayload;
        this.errorMessage = builder.errorMessage;
        this.retryCount = builder.retryCount;
        this.idempotencyKey = builder.idempotencyKey;
        this.cycleId = builder.cycleId;
        this.planId = builder.planId;
        this.createdAt = builder.createdAt;
        this.startedAt = builder.startedAt;
        this.finishedAt = builder.finishedAt;
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTaskId() {
        return taskId;
    }

    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }

    public String getExecutionId() {
        return executionId;
    }

    public void setExecutionId(String executionId) {
        this.executionId = executionId;
    }

    public ActionStatus getStatus() {
        return status;
    }

    public void setStatus(ActionStatus status) {
        this.status = status;
    }

    public String getActionType() {
        return actionType;
    }

    public void setActionType(String actionType) {
        this.actionType = actionType;
    }

    public String getTarget() {
        return target;
    }

    public void setTarget(String target) {
        this.target = target;
    }

    public String getParameters() {
        return parameters;
    }

    public void setParameters(String parameters) {
        this.parameters = parameters;
    }

    public String getRequestPayload() {
        return requestPayload;
    }

    public void setRequestPayload(String requestPayload) {
        this.requestPayload = requestPayload;
    }

    public String getResponsePayload() {
        return responsePayload;
    }

    public void setResponsePayload(String responsePayload) {
        this.responsePayload = responsePayload;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public int getRetryCount() {
        return retryCount;
    }

    public void setRetryCount(int retryCount) {
        this.retryCount = retryCount;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public void setIdempotencyKey(String idempotencyKey) {
        this.idempotencyKey = idempotencyKey;
    }

    public String getCycleId() {
        return cycleId;
    }

    public void setCycleId(String cycleId) {
        this.cycleId = cycleId;
    }

    public String getPlanId() {
        return planId;
    }

    public void setPlanId(String planId) {
        this.planId = planId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(Instant startedAt) {
        this.startedAt = startedAt;
    }

    public Instant getFinishedAt() {
        return finishedAt;
    }

    public void setFinishedAt(Instant finishedAt) {
        this.finishedAt = finishedAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public static Builder builder() {
        return new Builder();
    }

    public Builder with() {
        return new Builder()
                .id(this.id)
                .taskId(this.taskId)
                .executionId(this.executionId)
                .status(this.status)
                .actionType(this.actionType)
                .target(this.target)
                .parameters(this.parameters)
                .requestPayload(this.requestPayload)
                .responsePayload(this.responsePayload)
                .errorMessage(this.errorMessage)
                .retryCount(this.retryCount)
                .idempotencyKey(this.idempotencyKey)
                .cycleId(this.cycleId)
                .planId(this.planId)
                .createdAt(this.createdAt)
                .startedAt(this.startedAt)
                .finishedAt(this.finishedAt);
    }

    public static final class Builder {
        private String id;
        private String taskId;
        private String executionId;
        private ActionStatus status = ActionStatus.PENDING;
        private String actionType;
        private String target;
        private String parameters;
        private String requestPayload;
        private String responsePayload;
        private String errorMessage;
        private int retryCount = 0;
        private String idempotencyKey;
        private String cycleId;
        private String planId;
        private Instant createdAt;
        private Instant startedAt;
        private Instant finishedAt;

        public Builder id(String id) {
            this.id = id;
            return this;
        }

        public Builder taskId(String taskId) {
            this.taskId = taskId;
            return this;
        }

        public Builder executionId(String executionId) {
            this.executionId = executionId;
            return this;
        }

        public Builder status(ActionStatus status) {
            this.status = status;
            return this;
        }

        public Builder actionType(String actionType) {
            this.actionType = actionType;
            return this;
        }

        public Builder target(String target) {
            this.target = target;
            return this;
        }

        public Builder parameters(String parameters) {
            this.parameters = parameters;
            return this;
        }

        public Builder requestPayload(String requestPayload) {
            this.requestPayload = requestPayload;
            return this;
        }

        public Builder responsePayload(String responsePayload) {
            this.responsePayload = responsePayload;
            return this;
        }

        public Builder errorMessage(String errorMessage) {
            this.errorMessage = errorMessage;
            return this;
        }

        public Builder retryCount(int retryCount) {
            this.retryCount = retryCount;
            return this;
        }

        public Builder idempotencyKey(String idempotencyKey) {
            this.idempotencyKey = idempotencyKey;
            return this;
        }

        public Builder cycleId(String cycleId) {
            this.cycleId = cycleId;
            return this;
        }

        public Builder planId(String planId) {
            this.planId = planId;
            return this;
        }

        public Builder createdAt(Instant createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public Builder startedAt(Instant startedAt) {
            this.startedAt = startedAt;
            return this;
        }

        public Builder finishedAt(Instant finishedAt) {
            this.finishedAt = finishedAt;
            return this;
        }

        public ActionExecutionEntity build() {
            if (taskId == null || taskId.isBlank()) {
                throw new IllegalStateException("taskId is required");
            }
            if (executionId == null || executionId.isBlank()) {
                throw new IllegalStateException("executionId is required");
            }
            return new ActionExecutionEntity(this);
        }
    }

    @Override
    public String toString() {
        return "ActionExecutionEntity{" +
                "id='" + id + '\'' +
                ", taskId='" + taskId + '\'' +
                ", executionId='" + executionId + '\'' +
                ", status=" + status +
                ", actionType='" + actionType + '\'' +
                ", target='" + target + '\'' +
                ", cycleId='" + cycleId + '\'' +
                '}';
    }
}
