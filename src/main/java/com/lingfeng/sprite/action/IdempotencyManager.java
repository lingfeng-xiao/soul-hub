package com.lingfeng.sprite.action;

import java.util.concurrent.ConcurrentHashMap;

/**
 * IdempotencyManager - 幂等性控制
 */
public class IdempotencyManager {

    private final ConcurrentHashMap<String, ExecutionResult> executedTasks = new ConcurrentHashMap<>();

    /**
     * 检查任务是否已执行
     */
    public boolean isAlreadyExecuted(String idempotencyKey) {
        return executedTasks.containsKey(idempotencyKey);
    }

    /**
     * 标记任务为已执行
     */
    public void markAsExecuted(String idempotencyKey, Object result) {
        executedTasks.put(idempotencyKey, new ExecutionResult(true, result, null));
    }

    /**
     * 标记任务为失败
     */
    public void markAsFailed(String idempotencyKey, String error) {
        executedTasks.put(idempotencyKey, new ExecutionResult(false, null, error));
    }

    /**
     * 获取执行结果
     */
    public ExecutionResult getResult(String idempotencyKey) {
        return executedTasks.get(idempotencyKey);
    }

    /**
     * 执行结果记录
     */
    public record ExecutionResult(
            boolean success,
            Object result,
            String error
    ) {}
}
