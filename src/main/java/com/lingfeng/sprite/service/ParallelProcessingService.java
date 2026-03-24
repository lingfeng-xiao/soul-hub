package com.lingfeng.sprite.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.RecursiveTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * S24-3: 并行处理优化服务
 *
 * 提供并行执行独立认知任务的能力：
 * - 基于 ForkJoinPool 的并行处理
 * - 支持任务依赖管理
 * - 线程池管理与监控
 * - 可测量并行效率
 */
@Service
public class ParallelProcessingService {

    private static final Logger logger = LoggerFactory.getLogger(ParallelProcessingService.class);

    private static final int PARALLELISM = Runtime.getRuntime().availableProcessors();
    private static final int THRESHOLD = 5; // 任务合并阈值

    private final ForkJoinPool commonPool;
    private final Map<String, TaskDefinition> registeredTasks;
    private final AtomicLong totalTasksSubmitted;
    private final AtomicLong totalTasksCompleted;
    private final AtomicLong totalParallelTime;
    private final AtomicLong totalSequentialTime;

    // ==================== 核心记录定义 ====================

    /**
     * S24-3: 可执行任务
     *
     * @param <T> 任务返回类型
     */
    public record Task<T>(
        String id,
        Supplier<T> callable,
        List<String> dependencies,
        int priority
    ) {
        public Task(String id, Supplier<T> callable, List<String> dependencies, int priority) {
            this.id = id;
            this.callable = callable;
            this.dependencies = dependencies != null ? dependencies : List.of();
            this.priority = priority;
        }

        public Task(String id, Supplier<T> callable) {
            this(id, callable, List.of(), 0);
        }
    }

    /**
     * S24-3: 任务定义
     */
    public record TaskDefinition(
        String id,
        String name,
        TaskType type,
        Map<String, Object> config,
        Instant createdAt
    ) {
        public TaskDefinition(String id, String name, TaskType type, Map<String, Object> config) {
            this(id, name, type, config, Instant.now());
        }
    }

    /**
     * S24-3: 任务类型枚举
     */
    public enum TaskType {
        COGNITION,
        REASONING,
        MEMORY,
        PERCEPTION,
        ACTION,
        REFLECTION,
        EVOLUTION,
        CUSTOM
    }

    /**
     * S24-3: 执行器状态
     */
    public record ExecutorStatus(
        int activeThreads,
        int poolSize,
        int queueSize,
        long completedTasks,
        long submittedTasks,
        double averageParallelism,
        long totalParallelTimeMs,
        long totalSequentialTimeMs,
        double parallelismEfficiency,
        boolean isShutdown
    ) {}

    /**
     * S24-3: 任务执行结果
     */
    public record TaskResult<T>(
        String taskId,
        T result,
        boolean success,
        String error,
        long executionTimeMs,
        Instant completedAt
    ) {}

    /**
     * S24-3: 并行执行上下文
     */
    private record ParallelExecutionContext<T>(
        List<Task<T>> tasks,
        Map<String, TaskResult<T>> results,
        Set<String> completedTaskIds,
        long startTime
    ) {}

    public ParallelProcessingService() {
        this.commonPool = ForkJoinPool.commonPool();
        this.registeredTasks = new java.util.concurrent.ConcurrentHashMap<>();
        this.totalTasksSubmitted = new AtomicLong(0);
        this.totalTasksCompleted = new AtomicLong(0);
        this.totalParallelTime = new AtomicLong(0);
        this.totalSequentialTime = new AtomicLong(0);

        logger.info("ParallelProcessingService initialized with parallelism={}", PARALLELISM);
    }

    // ==================== 核心方法 ====================

    /**
     * S24-3: 并行执行任务
     *
     * @param tasks 任务列表
     * @return 任务结果列表
     */
    public <T> List<T> executeParallel(List<Task<T>> tasks) {
        if (tasks == null || tasks.isEmpty()) {
            return List.of();
        }

        long startTime = System.currentTimeMillis();
        totalTasksSubmitted.addAndGet(tasks.size());

        logger.debug("Starting parallel execution of {} tasks", tasks.size());

        // 识别独立任务和依赖任务
        List<Task<T>> independentTasks = new ArrayList<>();
        List<Task<T>> dependentTasks = new ArrayList<>();

        for (Task<T> task : tasks) {
            if (task.dependencies().isEmpty()) {
                independentTasks.add(task);
            } else {
                dependentTasks.add(task);
            }
        }

        // 并行执行独立任务
        List<T> results = new ArrayList<>();
        Map<String, T> resultMap = new java.util.concurrent.ConcurrentHashMap<>();

        if (!independentTasks.isEmpty()) {
            List<T> independentResults = executeIndependentTasks(independentTasks);
            for (int i = 0; i < independentTasks.size(); i++) {
                resultMap.put(independentTasks.get(i).id(), independentResults.get(i));
            }
            results.addAll(independentResults);
        }

        // 按依赖顺序执行依赖任务
        if (!dependentTasks.isEmpty()) {
            for (Task<T> task : dependentTasks) {
                // 等待依赖完成
                waitForDependencies(task.dependencies(), resultMap);

                // 执行任务
                long taskStart = System.currentTimeMillis();
                try {
                    T result = task.callable().get();
                    resultMap.put(task.id(), result);
                    results.add(result);
                    totalTasksCompleted.incrementAndGet();

                    long taskTime = System.currentTimeMillis() - taskStart;
                    totalParallelTime.addAndGet(taskTime);

                    logger.debug("Task {} completed in {}ms", task.id(), taskTime);
                } catch (Exception e) {
                    logger.error("Task {} failed: {}", task.id(), e.getMessage());
                    results.add(null);
                }
            }
        }

        long totalTime = System.currentTimeMillis() - startTime;
        long sequentialTime = tasks.size() * (totalTime / Math.max(tasks.size(), 1));
        totalSequentialTime.addAndGet(sequentialTime);

        logger.info("Parallel execution completed: {} tasks in {}ms (efficiency: {:.1f}%)",
            tasks.size(), totalTime, calculateEfficiency(totalTime, sequentialTime));

        return results;
    }

    /**
     * S24-3: 异步并行执行
     *
     * @param tasks 任务列表
     * @return CompletableFuture 包含任务结果
     */
    public <T> CompletableFuture<List<T>> executeAsync(List<Task<T>> tasks) {
        return CompletableFuture.supplyAsync(() -> executeParallel(tasks), commonPool)
            .whenComplete((results, error) -> {
                if (error != null) {
                    logger.error("Async parallel execution failed: {}", error.getMessage());
                } else {
                    logger.debug("Async parallel execution completed");
                }
            });
    }

    /**
     * S24-3: 注册任务定义
     *
     * @param definition 任务定义
     * @return 任务ID
     */
    public String registerTask(TaskDefinition definition) {
        if (definition == null || definition.id() == null) {
            throw new IllegalArgumentException("Task definition and ID cannot be null");
        }

        registeredTasks.put(definition.id(), definition);
        logger.debug("Registered task: id={}, name={}, type={}",
            definition.id(), definition.name(), definition.type());

        return definition.id();
    }

    /**
     * S24-3: 获取执行器状态
     *
     * @return 执行器状态
     */
    public ExecutorStatus getStatus() {
        ForkJoinPool.ForkJoinWorkerThreadFactory factory = commonPool.getFactory();
        int activeCount = commonPool.getActiveThreadCount();
        int poolSize = commonPool.getPoolSize();
        long completedCount = commonPool.getStealCount();

        long submitted = totalTasksSubmitted.get();
        long completed = totalTasksCompleted.get();
        long parallelTime = totalParallelTime.get();
        long sequentialTime = totalSequentialTime.get();

        double efficiency = calculateEfficiency(parallelTime, sequentialTime);

        return new ExecutorStatus(
            activeCount,
            poolSize,
            (int) commonPool.getQueuedTaskCount(),
            completed,
            submitted,
            calculateAverageParallelism(activeCount, poolSize),
            parallelTime,
            sequentialTime,
            efficiency,
            commonPool.isShutdown()
        );
    }

    // ==================== 辅助方法 ====================

    /**
     * 执行独立任务（使用 ForkJoin）
     */
    private <T> List<T> executeIndependentTasks(List<Task<T>> tasks) {
        if (tasks.size() <= THRESHOLD) {
            // 小任务直接串行执行
            return tasks.stream()
                .map(task -> {
                    long taskStart = System.currentTimeMillis();
                    try {
                        T result = task.callable().get();
                        totalTasksCompleted.incrementAndGet();
                        return result;
                    } catch (Exception e) {
                        logger.error("Task {} failed: {}", task.id(), e.getMessage());
                        return null;
                    }
                })
                .collect(Collectors.toList());
        }

        // 大任务使用 ForkJoin 并行执行
        ParallelTask<T> rootTask = new ParallelTask<>(tasks, 0, tasks.size());
        return commonPool.invoke(rootTask);
    }

    /**
     * 等待依赖任务完成
     */
    private <T> void waitForDependencies(List<String> dependencies, Map<String, T> resultMap) {
        if (dependencies == null || dependencies.isEmpty()) {
            return;
        }

        int maxWaitTime = 30000; // 30秒超时
        int checkInterval = 10; // 10ms检查间隔
        int waited = 0;

        Set<String> missing = new HashSet<>(dependencies);
        while (!missing.isEmpty() && waited < maxWaitTime) {
            missing.removeAll(resultMap.keySet());
            if (!missing.isEmpty()) {
                try {
                    Thread.sleep(checkInterval);
                    waited += checkInterval;
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }

        if (!missing.isEmpty()) {
            logger.warn("Some dependencies not met after {}ms: {}", waited, missing);
        }
    }

    /**
     * 计算并行效率
     */
    private double calculateEfficiency(long parallelTime, long sequentialTime) {
        if (sequentialTime <= 0 || parallelTime <= 0) {
            return 0.0;
        }
        double speedup = (double) sequentialTime / parallelTime;
        int cores = PARALLELISM;
        return Math.min(100.0, (speedup / cores) * 100);
    }

    /**
     * 计算平均并行度
     */
    private double calculateAverageParallelism(int activeThreads, int poolSize) {
        if (poolSize <= 0) {
            return 0.0;
        }
        return (double) activeThreads / poolSize;
    }

    /**
     * 获取注册的任务定义
     */
    public TaskDefinition getRegisteredTask(String taskId) {
        return registeredTasks.get(taskId);
    }

    /**
     * 获取所有注册任务
     */
    public List<TaskDefinition> getAllRegisteredTasks() {
        return new ArrayList<>(registeredTasks.values());
    }

    /**
     * 移除注册的任务
     */
    public boolean unregisterTask(String taskId) {
        return registeredTasks.remove(taskId) != null;
    }

    /**
     * 创建任务（建造者模式）
     */
    public <T> Task<T> createTask(String id, Supplier<T> callable) {
        return new Task<>(id, callable);
    }

    /**
     * 创建带依赖的任务
     */
    public <T> Task<T> createTaskWithDependencies(String id, Supplier<T> callable, List<String> dependencies) {
        return new Task<>(id, callable, dependencies, 0);
    }

    /**
     * 创建带优先级的任务
     */
    public <T> Task<T> createTaskWithPriority(String id, Supplier<T> callable, int priority) {
        return new Task<>(id, callable, List.of(), priority);
    }

    // ==================== ForkJoin 任务类 ====================

    /**
     * S24-3: 并行任务（用于 ForkJoinPool）
     */
    private class ParallelTask<T> extends RecursiveTask<List<T>> {

        private final List<Task<T>> tasks;
        private final int start;
        private final int end;

        ParallelTask(List<Task<T>> tasks, int start, int end) {
            this.tasks = tasks;
            this.start = start;
            this.end = end;
        }

        @Override
        protected List<T> compute() {
            int size = end - start;

            if (size <= THRESHOLD) {
                // 小任务直接执行
                List<T> results = new ArrayList<>(size);
                for (int i = start; i < end; i++) {
                    Task<T> task = tasks.get(i);
                    long taskStart = System.currentTimeMillis();
                    try {
                        T result = task.callable().get();
                        results.add(result);
                        totalTasksCompleted.incrementAndGet();

                        long taskTime = System.currentTimeMillis() - taskStart;
                        totalParallelTime.addAndGet(taskTime);
                    } catch (Exception e) {
                        logger.error("Task {} failed: {}", task.id(), e.getMessage());
                        results.add(null);
                    }
                }
                return results;
            }

            // 大任务拆分
            int mid = start + size / 2;
            ParallelTask<T> leftTask = new ParallelTask<>(tasks, start, mid);
            ParallelTask<T> rightTask = new ParallelTask<>(tasks, mid, end);

            leftTask.fork();
            List<T> rightResults = rightTask.compute();
            List<T> leftResults = leftTask.join();

            // 合并结果
            List<T> results = new ArrayList<>(size);
            results.addAll(leftResults);
            results.addAll(rightResults);
            return results;
        }
    }

    // ==================== 便捷执行方法 ====================

    /**
     * S24-3: 执行单个任务
     */
    public <T> T executeSingle(Task<T> task) {
        long start = System.currentTimeMillis();
        try {
            T result = task.callable().get();
            totalTasksCompleted.incrementAndGet();
            totalParallelTime.addAndGet(System.currentTimeMillis() - start);
            return result;
        } catch (Exception e) {
            logger.error("Task {} failed: {}", task.id(), e.getMessage());
            return null;
        }
    }

    /**
     * S24-3: 执行多个独立任务
     */
    @SafeVarargs
    public final <T> List<T> executeIndependent(Task<T>... tasks) {
        return executeParallel(Arrays.asList(tasks));
    }

    /**
     * S24-3: 创建认知任务
     */
    public Task<Void> createCognitionTask(String id, Runnable cognitionLogic) {
        return new Task<>(id, () -> {
            cognitionLogic.run();
            return null;
        }, List.of(), 0);
    }

    /**
     * S24-3: 创建推理任务
     */
    public <T> Task<T> createReasoningTask(String id, Supplier<T> reasoningLogic) {
        return new Task<>(id, reasoningLogic, List.of(), 1);
    }

    /**
     * S24-3: 创建记忆任务
     */
    public <T> Task<T> createMemoryTask(String id, Supplier<T> memoryLogic, String... dependencyTaskIds) {
        return new Task<>(id, memoryLogic, Arrays.asList(dependencyTaskIds), 0);
    }

    // ==================== 生命周期方法 ====================

    /**
     * 关闭线程池
     */
    public void shutdown() {
        logger.info("Shutting down ParallelProcessingService");
        commonPool.shutdown();
        try {
            if (!commonPool.awaitTermination(60, TimeUnit.SECONDS)) {
                commonPool.shutdownNow();
            }
        } catch (InterruptedException e) {
            commonPool.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    /**
     * 强制关闭线程池
     */
    public void forceShutdown() {
        logger.warn("Force shutting down ParallelProcessingService");
        commonPool.shutdownNow();
    }

    /**
     * 获取服务信息
     */
    public String getServiceInfo() {
        ExecutorStatus status = getStatus();
        return String.format(
            "ParallelProcessingService[poolSize=%d, active=%d, completed=%d, efficiency=%.1f%%]",
            status.poolSize(),
            status.activeThreads(),
            status.completedTasks(),
            status.parallelismEfficiency()
        );
    }
}
