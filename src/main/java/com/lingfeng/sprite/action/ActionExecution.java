package com.lingfeng.sprite.action;

import java.time.Instant;
import java.util.UUID;

/**
 * ActionExecution - 动作执行记录
 */
public final class ActionExecution {

    public enum ExecutionStatus {
        RUNNING, SUCCEEDED, FAILED
    }

    private final String executionId;
    private final String taskId;
    private final ExecutionStatus status;
    private final Object requestPayload;
    private final Object responsePayload;
    private final String errorMessage;
    private final int retryCount;
    private final Instant startedAt;
    private final Instant finishedAt;

    private ActionExecution(Builder builder) {
        this.executionId = builder.executionId;
        this.taskId = builder.taskId;
        this.status = builder.status;
        this.requestPayload = builder.requestPayload;
        this.responsePayload = builder.responsePayload;
        this.errorMessage = builder.errorMessage;
        this.retryCount = builder.retryCount;
        this.startedAt = builder.startedAt;
        this.finishedAt = builder.finishedAt;
    }

    public String executionId() {
        return executionId;
    }

    public String taskId() {
        return taskId;
    }

    public ExecutionStatus status() {
        return status;
    }

    public Object requestPayload() {
        return requestPayload;
    }

    public Object responsePayload() {
        return responsePayload;
    }

    public String errorMessage() {
        return errorMessage;
    }

    public int retryCount() {
        return retryCount;
    }

    public Instant startedAt() {
        return startedAt;
    }

    public Instant finishedAt() {
        return finishedAt;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static Builder builder(ActionExecution other) {
        return new Builder()
                .executionId(other.executionId)
                .taskId(other.taskId)
                .status(other.status)
                .requestPayload(other.requestPayload)
                .responsePayload(other.responsePayload)
                .errorMessage(other.errorMessage)
                .retryCount(other.retryCount)
                .startedAt(other.startedAt)
                .finishedAt(other.finishedAt);
    }

    public static class Builder {
        private String executionId = UUID.randomUUID().toString();
        private String taskId;
        private ExecutionStatus status = ExecutionStatus.RUNNING;
        private Object requestPayload;
        private Object responsePayload;
        private String errorMessage;
        private int retryCount = 0;
        private Instant startedAt = Instant.now();
        private Instant finishedAt;

        public Builder executionId(String executionId) {
            this.executionId = executionId;
            return this;
        }

        public Builder taskId(String taskId) {
            this.taskId = taskId;
            return this;
        }

        public Builder status(ExecutionStatus status) {
            this.status = status;
            return this;
        }

        public Builder requestPayload(Object requestPayload) {
            this.requestPayload = requestPayload;
            return this;
        }

        public Builder responsePayload(Object responsePayload) {
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

        public Builder startedAt(Instant startedAt) {
            this.startedAt = startedAt;
            return this;
        }

        public Builder finishedAt(Instant finishedAt) {
            this.finishedAt = finishedAt;
            return this;
        }

        public ActionExecution build() {
            return new ActionExecution(this);
        }
    }
}
