package com.lingfeng.sprite.action;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * ActionTask - 动作任务
 * 代表一个可执行的原子动作
 */
public final class ActionTask {

    public enum ActionType {
        SEND_MESSAGE,    // 发送消息
        QUERY_MEMORY,   // 查询记忆
        RECORD_MEMORY,  // 记录记忆
        CALL_API,       // 调用API
        NOTIFY,         // 通知
        CUSTOM          // 自定义
    }

    public enum TaskStatus {
        PENDING,        // 待执行
        DISPATCHED,     // 已派发
        RUNNING,        // 执行中
        SUCCEEDED,      // 成功
        FAILED,         // 失败
        COMPENSATED     // 已补偿
    }

    private final String taskId;
    private final String planId;
    private final ActionType type;
    private final String adapter;
    private final Map<String, Object> parameters;
    private final TaskStatus status;
    private final int retryCount;
    private final String result;
    private final Instant createdAt;
    private final Instant startedAt;
    private final Instant finishedAt;

    private ActionTask(Builder builder) {
        this.taskId = builder.taskId;
        this.planId = builder.planId;
        this.type = builder.type;
        this.adapter = builder.adapter;
        this.parameters = builder.parameters;
        this.status = builder.status;
        this.retryCount = builder.retryCount;
        this.result = builder.result;
        this.createdAt = builder.createdAt;
        this.startedAt = builder.startedAt;
        this.finishedAt = builder.finishedAt;
    }

    public String taskId() {
        return taskId;
    }

    public String planId() {
        return planId;
    }

    public ActionType type() {
        return type;
    }

    public String adapter() {
        return adapter;
    }

    public Map<String, Object> parameters() {
        return parameters;
    }

    public TaskStatus status() {
        return status;
    }

    public int retryCount() {
        return retryCount;
    }

    public String result() {
        return result;
    }

    public Instant createdAt() {
        return createdAt;
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

    public static Builder builder(ActionTask other) {
        return new Builder()
                .taskId(other.taskId)
                .planId(other.planId)
                .type(other.type)
                .adapter(other.adapter)
                .parameters(other.parameters)
                .status(other.status)
                .retryCount(other.retryCount)
                .result(other.result)
                .createdAt(other.createdAt)
                .startedAt(other.startedAt)
                .finishedAt(other.finishedAt);
    }

    public static class Builder {
        private String taskId = UUID.randomUUID().toString();
        private String planId;
        private ActionType type;
        private String adapter;
        private Map<String, Object> parameters;
        private TaskStatus status = TaskStatus.PENDING;
        private int retryCount = 0;
        private String result;
        private Instant createdAt = Instant.now();
        private Instant startedAt;
        private Instant finishedAt;

        public Builder taskId(String taskId) {
            this.taskId = taskId;
            return this;
        }

        public Builder planId(String planId) {
            this.planId = planId;
            return this;
        }

        public Builder type(ActionType type) {
            this.type = type;
            return this;
        }

        public Builder adapter(String adapter) {
            this.adapter = adapter;
            return this;
        }

        public Builder parameters(Map<String, Object> parameters) {
            this.parameters = parameters;
            return this;
        }

        public Builder status(TaskStatus status) {
            this.status = status;
            return this;
        }

        public Builder retryCount(int retryCount) {
            this.retryCount = retryCount;
            return this;
        }

        public Builder result(String result) {
            this.result = result;
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

        public ActionTask build() {
            return new ActionTask(this);
        }
    }
}
