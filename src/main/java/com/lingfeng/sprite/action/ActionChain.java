package com.lingfeng.sprite.action;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * S7-4: 动作链编排器
 *
 * 支持:
 * - 顺序执行: 按顺序执行动作，上一个完成才执行下一个
 * - 并行执行: 多个动作同时执行
 * - 条件执行: 根据前一个动作结果决定是否执行
 * - 回滚支持: 失败时撤销已执行的动作
 */
public class ActionChain {

    private static final Logger logger = LoggerFactory.getLogger(ActionChain.class);

    /**
     * 动作链节点
     */
    public record ChainNode(
            String actionName,
            Map<String, Object> params,
            ExecutionMode mode,
            Runnable rollbackAction  // 可选的回滚动作
    ) {}

    /**
     * 执行模式
     */
    public enum ExecutionMode {
        SEQUENTIAL,   // 顺序执行
        PARALLEL      // 并行执行
    }

    /**
     * 动作链执行结果
     */
    public record ChainResult(
            boolean success,
            String message,
            Instant startTime,
            Instant endTime,
            List<NodeResult> nodeResults,
            boolean rolledBack
    ) {
        public int totalNodes() { return nodeResults != null ? nodeResults.size() : 0; }
        public int successCount() { return nodeResults != null ? (int) nodeResults.stream().filter(r -> r.success()).count() : 0; }
    }

    /**
     * 节点执行结果
     */
    public record NodeResult(
            String actionName,
            boolean success,
            String message,
            Instant startTime,
            Instant endTime,
            Object data
    ) {}

    private final List<ChainNode> nodes;
    private final ActionExecutor actionExecutor;
    private final int timeoutSeconds;

    public ActionChain(ActionExecutor actionExecutor) {
        this(actionExecutor, 60); // 默认60秒超时
    }

    public ActionChain(ActionExecutor actionExecutor, int timeoutSeconds) {
        this.actionExecutor = actionExecutor;
        this.timeoutSeconds = timeoutSeconds;
        this.nodes = new ArrayList<>();
    }

    /**
     * 添加动作节点到链
     */
    public ActionChain addNode(String actionName, Map<String, Object> params) {
        return addNode(actionName, params, ExecutionMode.SEQUENTIAL, null);
    }

    /**
     * 添加动作节点到链 (带回滚)
     */
    public ActionChain addNode(String actionName, Map<String, Object> params, Runnable rollbackAction) {
        return addNode(actionName, params, ExecutionMode.SEQUENTIAL, rollbackAction);
    }

    /**
     * 添加动作节点到链
     */
    public ActionChain addNode(String actionName, Map<String, Object> params, ExecutionMode mode, Runnable rollbackAction) {
        nodes.add(new ChainNode(actionName, params, mode, rollbackAction));
        return this;
    }

    /**
     * 添加顺序执行的动作
     */
    public ActionChain addSequential(String actionName, Map<String, Object> params) {
        return addNode(actionName, params, ExecutionMode.SEQUENTIAL, null);
    }

    /**
     * 添加并行执行的动作
     */
    public ActionChain addParallel(String actionName, Map<String, Object> params) {
        return addNode(actionName, params, ExecutionMode.PARALLEL, null);
    }

    /**
     * 执行动作链
     */
    public ChainResult execute() {
        return execute(null);
    }

    /**
     * 执行动作链 (带上下文)
     */
    public ChainResult execute(Map<String, Object> context) {
        if (nodes.isEmpty()) {
            return new ChainResult(true, "Empty chain", Instant.now(), Instant.now(), List.of(), false);
        }

        Instant startTime = Instant.now();
        List<NodeResult> nodeResults = new ArrayList<>();
        boolean rolledBack = false;

        logger.info("=== Executing Action Chain ===");
        logger.info("Total nodes: {}", nodes.size());
        logger.info("Timeout: {} seconds", timeoutSeconds);
        logger.info("=============================");

        try {
            // 分离顺序和并行节点
            List<ChainNode> sequentialNodes = new ArrayList<>();
            List<ChainNode> parallelNodes = new ArrayList<>();

            for (ChainNode node : nodes) {
                if (node.mode() == ExecutionMode.PARALLEL) {
                    parallelNodes.add(node);
                } else {
                    sequentialNodes.add(node);
                }
            }

            // 执行顺序节点
            for (ChainNode node : sequentialNodes) {
                NodeResult result = executeNode(node, context);
                nodeResults.add(result);

                if (!result.success()) {
                    logger.warn("Sequential node '{}' failed: {}", node.actionName(), result.message());
                    // 顺序执行模式下，失败则停止
                    break;
                }
            }

            // 执行并行节点
            if (!parallelNodes.isEmpty()) {
                List<NodeResult> parallelResults = executeParallelNodes(parallelNodes, context);
                nodeResults.addAll(parallelResults);
            }

        } catch (Exception e) {
            logger.error("Action chain execution failed: {}", e.getMessage());
            // 执行回滚
            rolledBack = rollback(nodeResults);
            return new ChainResult(false, "Chain execution failed: " + e.getMessage(), startTime, Instant.now(), nodeResults, rolledBack);
        }

        Instant endTime = Instant.now();
        boolean allSuccess = nodeResults.stream().allMatch(NodeResult::success);

        if (allSuccess) {
            logger.info("=== Action Chain Completed Successfully ===");
            return new ChainResult(true, "Chain executed successfully", startTime, endTime, nodeResults, false);
        } else {
            logger.warn("=== Action Chain Completed with Failures ===");
            // 回滚已执行的动作
            rolledBack = rollback(nodeResults);
            return new ChainResult(false, "Some nodes failed, rolled back", startTime, endTime, nodeResults, rolledBack);
        }
    }

    /**
     * 执行单个节点
     */
    private NodeResult executeNode(ChainNode node, Map<String, Object> context) {
        Instant startTime = Instant.now();
        logger.info("Executing node: {}", node.actionName());

        try {
            ActionResult result = actionExecutor.executeTool(node.actionName(), node.params());

            Instant endTime = Instant.now();
            return new NodeResult(
                    node.actionName(),
                    result.success(),
                    result.message(),
                    startTime,
                    endTime,
                    result.data()
            );

        } catch (Exception e) {
            logger.error("Node '{}' execution failed: {}", node.actionName(), e.getMessage());
            return new NodeResult(
                    node.actionName(),
                    false,
                    "Execution failed: " + e.getMessage(),
                    startTime,
                    Instant.now(),
                    null
            );
        }
    }

    /**
     * 并行执行多个节点
     */
    private List<NodeResult> executeParallelNodes(List<ChainNode> nodes, Map<String, Object> context) throws ExecutionException, InterruptedException, TimeoutException {
        List<CompletableFuture<NodeResult>> futures = new ArrayList<>();

        for (ChainNode node : nodes) {
            CompletableFuture<NodeResult> future = CompletableFuture.supplyAsync(() -> executeNode(node, context));
            futures.add(future);
        }

        // 等待所有并行任务完成
        CompletableFuture<Void> allFutures = CompletableFuture.allOf(
                futures.toArray(new CompletableFuture[0])
        );

        allFutures.get(timeoutSeconds, TimeUnit.SECONDS);

        List<NodeResult> results = new ArrayList<>();
        for (CompletableFuture<NodeResult> future : futures) {
            results.add(future.get());
        }

        return results;
    }

    /**
     * 回滚已执行的动作
     */
    private boolean rollback(List<NodeResult> nodeResults) {
        logger.info("=== Rolling back action chain ===");
        boolean anyRolledBack = false;

        // 逆序执行回滚动作
        for (int i = nodeResults.size() - 1; i >= 0; i--) {
            NodeResult result = nodeResults.get(i);
            if (result.success() && i < nodes.size()) {
                ChainNode node = nodes.get(i);
                if (node.rollbackAction() != null) {
                    try {
                        logger.info("Rolling back node: {}", node.actionName());
                        node.rollbackAction().run();
                        anyRolledBack = true;
                    } catch (Exception e) {
                        logger.error("Rollback for node '{}' failed: {}", node.actionName(), e.getMessage());
                    }
                }
            }
        }

        logger.info("=== Rollback complete ===");
        return anyRolledBack;
    }

    /**
     * 获取链中的节点数
     */
    public int size() {
        return nodes.size();
    }

    /**
     * 清空动作链
     */
    public void clear() {
        nodes.clear();
    }
}
