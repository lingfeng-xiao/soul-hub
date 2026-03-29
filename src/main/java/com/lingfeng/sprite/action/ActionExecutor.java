package com.lingfeng.sprite.action;

import com.lingfeng.sprite.action.entity.ActionExecutionEntity;
import com.lingfeng.sprite.action.repository.ActionExecutionRepository;
import com.lingfeng.sprite.runtime.DomainEventBus;
import com.lingfeng.sprite.runtime.event.ActionDispatchedEvent;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * ActionExecutor - Phase 10: Action System Integration
 *
 * Integrates Phase 4 ActionTask/ActionExecution with CognitionOrchestrator.
 * This service replaces the legacy ActionChain for cognitive action execution.
 *
 * Features:
 * - State machine: PENDING -> RUNNING -> SUCCEEDED/FAILED/COMPENSATED
 * - Idempotency control via IdempotencyManager
 * - Compensation handling via CompensationHandler
 * - Domain event publishing via DomainEventBus
 *
 * Dependencies:
 * - IdempotencyManager (Phase 4)
 * - CompensationHandler (Phase 4)
 * - DomainEventBus (Phase 1)
 * - CognitionOrchestrator (Phase 9)
 */
@Service("lifeActionExecutor")
@Profile("action-v2")
public class ActionExecutor {

    private static final Logger logger = LoggerFactory.getLogger(ActionExecutor.class);

    private final ActionExecutionRepository repository;
    private final IdempotencyManager idempotencyManager;
    private final CompensationHandler compensationHandler;
    private final DomainEventBus eventBus;
    private final ObjectMapper objectMapper;

    // Plugin registry for action execution
    private final Map<String, ActionPlugin> actionPlugins;

    public ActionExecutor(
            ActionExecutionRepository repository,
            IdempotencyManager idempotencyManager,
            CompensationHandler compensationHandler,
            DomainEventBus eventBus,
            ObjectMapper objectMapper) {
        this.repository = repository;
        this.idempotencyManager = idempotencyManager;
        this.compensationHandler = compensationHandler;
        this.eventBus = eventBus;
        this.objectMapper = objectMapper;
        this.actionPlugins = new ConcurrentHashMap<>();

        logger.info("ActionExecutor initialized with IdempotencyManager and CompensationHandler");
    }

    /**
     * Register an action plugin for execution
     */
    public void registerPlugin(String name, ActionPlugin plugin) {
        actionPlugins.put(name, plugin);
        logger.info("Registered action plugin: {}", name);
    }

    /**
     * Execute an ActionTask with full state machine support
     *
     * @param task The action task to execute
     * @param cycleId The cognition cycle ID this action belongs to
     * @return Execution result
     */
    @Transactional
    public ActionExecution execute(ActionTask task, String cycleId) {
        String taskId = task.taskId();
        String executionId = UUID.randomUUID().toString();

        logger.info("Executing ActionTask: taskId={}, type={}, cycleId={}",
                taskId, task.type(), cycleId);

        // Generate idempotency key based on actionType + target + timestamp
        String idempotencyKey = generateIdempotencyKey(task, cycleId);

        // Check idempotency - prevent duplicate execution
        if (idempotencyManager.isAlreadyExecuted(idempotencyKey)) {
            logger.warn("Action already executed (idempotency check): {}", idempotencyKey);
            IdempotencyManager.ExecutionResult cachedResult = idempotencyManager.getResult(idempotencyKey);
            return ActionExecution.builder()
                    .executionId(executionId)
                    .taskId(taskId)
                    .status(cachedResult.success() ? ActionExecution.ExecutionStatus.SUCCEEDED : ActionExecution.ExecutionStatus.FAILED)
                    .errorMessage(cachedResult.error())
                    .build();
        }

        // Create execution entity and persist
        ActionExecutionEntity entity = createExecutionEntity(task, executionId, cycleId, idempotencyKey);
        entity = repository.save(entity);

        // Transition to RUNNING
        entity.setStatus(ActionStatus.RUNNING);
        entity.setStartedAt(Instant.now());
        entity = repository.save(entity);

        // Publish ActionDispatchedEvent
        publishActionDispatchedEvent(task, cycleId, entity);

        try {
            // Execute the action via plugin
            ActionResult result = executeAction(task);

            if (result.success()) {
                // Transition to SUCCEEDED
                entity.setStatus(ActionStatus.SUCCEEDED);
                entity.setResponsePayload(serialize(result.data()));
                entity.setFinishedAt(Instant.now());
                repository.save(entity);

                idempotencyManager.markAsExecuted(idempotencyKey, result.data());

                logger.info("Action succeeded: taskId={}, executionId={}", taskId, executionId);

                return ActionExecution.builder()
                        .executionId(executionId)
                        .taskId(taskId)
                        .status(ActionExecution.ExecutionStatus.SUCCEEDED)
                        .responsePayload(result.data())
                        .startedAt(entity.getStartedAt())
                        .finishedAt(entity.getFinishedAt())
                        .build();
            } else {
                // Transition to FAILED
                return handleFailure(entity, task, result.message(), idempotencyKey);
            }
        } catch (Exception e) {
            logger.error("Action execution threw exception: taskId={}, error={}", taskId, e.getMessage());
            return handleFailure(entity, task, e.getMessage(), idempotencyKey);
        }
    }

    /**
     * Handle action failure with compensation support
     */
    private ActionExecution handleFailure(ActionExecutionEntity entity, ActionTask task, String error, String idempotencyKey) {
        entity.setStatus(ActionStatus.FAILED);
        entity.setErrorMessage(error);
        entity.setFinishedAt(Instant.now());
        repository.save(entity);

        idempotencyManager.markAsFailed(idempotencyKey, error);

        // Attempt compensation
        try {
            compensationHandler.compensate(task);
            entity.setStatus(ActionStatus.COMPENSATED);
            repository.save(entity);
            logger.info("Action compensated after failure: taskId={}", task.taskId());
        } catch (Exception e) {
            logger.error("Compensation failed for taskId={}: {}", task.taskId(), e.getMessage());
        }

        return ActionExecution.builder()
                .executionId(entity.getExecutionId())
                .taskId(task.taskId())
                .status(ActionExecution.ExecutionStatus.FAILED)
                .errorMessage(error)
                .startedAt(entity.getStartedAt())
                .finishedAt(entity.getFinishedAt())
                .build();
    }

    /**
     * Execute action via registered plugin
     */
    private ActionResult executeAction(ActionTask task) {
        ActionPlugin plugin = actionPlugins.get(task.type().name());

        if (plugin == null) {
            // Fallback: look for adapter-based plugin
            plugin = actionPlugins.get(task.adapter());
        }

        if (plugin == null) {
            logger.warn("No plugin found for action type: {}, adapter: {}",
                    task.type(), task.adapter());
            return ActionResult.failure("No plugin found for action type: " + task.type());
        }

        try {
            Map<String, Object> params = new ConcurrentHashMap<>(task.parameters());
            params.put("timestamp", Instant.now());
            return plugin.execute(params);
        } catch (Exception e) {
            logger.error("Plugin execution failed: {}", e.getMessage());
            return ActionResult.failure(e.getMessage());
        }
    }

    /**
     * Generate idempotency key based on actionType + target + timestamp
     */
    private String generateIdempotencyKey(ActionTask task, String cycleId) {
        // Use minute-level timestamp for deduplication window
        long timestampMinute = Instant.now().getEpochSecond() / 60;
        String target = task.parameters() != null ?
                String.valueOf(task.parameters().getOrDefault("target", "none")) : "none";
        return String.format("%s:%s:%s:%d", task.type().name(), target, cycleId, timestampMinute);
    }

    /**
     * Create execution entity from task
     */
    private ActionExecutionEntity createExecutionEntity(ActionTask task, String executionId, String cycleId, String idempotencyKey) {
        return ActionExecutionEntity.builder()
                .taskId(task.taskId())
                .executionId(executionId)
                .status(ActionStatus.PENDING)
                .actionType(task.type().name())
                .parameters(serialize(task.parameters()))
                .idempotencyKey(idempotencyKey)
                .cycleId(cycleId)
                .planId(task.planId())
                .retryCount(task.retryCount())
                .createdAt(Instant.now())
                .build();
    }

    /**
     * Publish ActionDispatchedEvent to DomainEventBus
     */
    private void publishActionDispatchedEvent(ActionTask task, String cycleId, ActionExecutionEntity entity) {
        try {
            ActionDispatchedEvent event = ActionDispatchedEvent.builder()
                    .cycleId(cycleId)
                    .actionType(task.type().name())
                    .actionContent(serialize(task.parameters()))
                    .target(task.parameters() != null ?
                            String.valueOf(task.parameters().getOrDefault("target", "")) : null)
                    .build();

            eventBus.publish(event);
            logger.debug("Published ActionDispatchedEvent: {}", event.getEventId());
        } catch (Exception e) {
            logger.warn("Failed to publish ActionDispatchedEvent: {}", e.getMessage());
        }
    }

    /**
     * Get execution by ID
     */
    public Optional<ActionExecutionEntity> getExecution(String executionId) {
        return repository.findByExecutionId(executionId);
    }

    /**
     * Get executions by task ID
     */
    public java.util.List<ActionExecutionEntity> getExecutionsByTaskId(String taskId) {
        return repository.findByTaskId(taskId);
    }

    /**
     * Get executions by cycle ID
     */
    public java.util.List<ActionExecutionEntity> getExecutionsByCycleId(String cycleId) {
        return repository.findByCycleId(cycleId);
    }

    /**
     * Serialize object to JSON string
     */
    private String serialize(Object obj) {
        if (obj == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            logger.warn("Failed to serialize object: {}", e.getMessage());
            return obj.toString();
        }
    }

    /**
     * Get current status of an execution
     */
    public Optional<ActionStatus> getExecutionStatus(String executionId) {
        return repository.findByExecutionId(executionId)
                .map(ActionExecutionEntity::getStatus);
    }

    /**
     * Check if an action has been executed (idempotency check)
     */
    public boolean isAlreadyExecuted(ActionTask task, String cycleId) {
        String idempotencyKey = generateIdempotencyKey(task, cycleId);
        return idempotencyManager.isAlreadyExecuted(idempotencyKey);
    }
}
