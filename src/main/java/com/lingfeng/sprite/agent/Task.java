package com.lingfeng.sprite.agent;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Task definition for inter-agent communication
 */
public record Task(
    String taskId,
    String type,
    String description,
    Map<String, Object> parameters,
    TaskStatus status,
    Object result,
    String error,
    Instant createdAt,
    Instant completedAt,
    int priority
) {
    public enum TaskStatus {
        PENDING,
        RUNNING,
        COMPLETED,
        FAILED,
        CANCELLED
    }

    public static Task create(String type, String description, Map<String, Object> parameters) {
        return new Task(
            UUID.randomUUID().toString(),
            type,
            description,
            parameters,
            TaskStatus.PENDING,
            null,
            null,
            Instant.now(),
            null,
            0
        );
    }

    public static Task create(String type, String description, Map<String, Object> parameters, int priority) {
        return new Task(
            UUID.randomUUID().toString(),
            type,
            description,
            parameters,
            TaskStatus.PENDING,
            null,
            null,
            Instant.now(),
            null,
            priority
        );
    }

    public Task withStatus(TaskStatus status) {
        return new Task(taskId, type, description, parameters, status, result, error, createdAt,
            status == TaskStatus.COMPLETED || status == TaskStatus.FAILED ? Instant.now() : completedAt, priority);
    }

    public Task withResult(Object result) {
        return new Task(taskId, type, description, parameters, TaskStatus.COMPLETED, result, error, createdAt,
            Instant.now(), priority);
    }

    public Task withError(String error) {
        return new Task(taskId, type, description, parameters, TaskStatus.FAILED, null, error, createdAt,
            Instant.now(), priority);
    }
}
