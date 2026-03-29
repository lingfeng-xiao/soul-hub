package com.lingfeng.sprite.action.repository;

import com.lingfeng.sprite.action.ActionStatus;
import com.lingfeng.sprite.action.entity.ActionExecutionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * ActionExecutionRepository - JPA repository for ActionExecutionEntity
 *
 * Provides CRUD operations and custom queries for action execution records.
 */
@Repository
public interface ActionExecutionRepository extends JpaRepository<ActionExecutionEntity, String> {

    /**
     * Find executions by task ID
     */
    List<ActionExecutionEntity> findByTaskId(String taskId);

    /**
     * Find executions by execution ID
     */
    Optional<ActionExecutionEntity> findByExecutionId(String executionId);

    /**
     * Find executions by status
     */
    List<ActionExecutionEntity> findByStatus(ActionStatus status);

    /**
     * Find executions by cycle ID
     */
    List<ActionExecutionEntity> findByCycleId(String cycleId);

    /**
     * Find executions by idempotency key
     */
    Optional<ActionExecutionEntity> findByIdempotencyKey(String idempotencyKey);

    /**
     * Check if an execution with the given idempotency key exists
     */
    boolean existsByIdempotencyKey(String idempotencyKey);

    /**
     * Find executions by task ID and status
     */
    List<ActionExecutionEntity> findByTaskIdAndStatus(String taskId, ActionStatus status);

    /**
     * Find executions created after a certain time
     */
    List<ActionExecutionEntity> findByCreatedAtAfter(Instant createdAt);

    /**
     * Find pending executions that have been waiting too long (for timeout handling)
     */
    @Query("SELECT e FROM ActionExecutionEntity e WHERE e.status = :status AND e.createdAt < :threshold")
    List<ActionExecutionEntity> findStalePendingExecutions(
            @Param("status") ActionStatus status,
            @Param("threshold") Instant threshold
    );

    /**
     * Find executions by plan ID ordered by creation time
     */
    List<ActionExecutionEntity> findByPlanIdOrderByCreatedAtAsc(String planId);

    /**
     * Count executions by status for a specific cycle
     */
    @Query("SELECT COUNT(e) FROM ActionExecutionEntity e WHERE e.cycleId = :cycleId AND e.status = :status")
    long countByCycleIdAndStatus(
            @Param("cycleId") String cycleId,
            @Param("status") ActionStatus status
    );

    /**
     * Find failed executions that haven't been compensated yet
     */
    @Query("SELECT e FROM ActionExecutionEntity e WHERE e.status = 'FAILED' AND e.cycleId = :cycleId ORDER BY e.createdAt DESC")
    List<ActionExecutionEntity> findUncompensatedFailures(@Param("cycleId") String cycleId);

    /**
     * Delete executions older than a certain time (for cleanup)
     */
    void deleteByCreatedAtBefore(Instant threshold);

    /**
     * Find recent executions across all cycles
     */
    List<ActionExecutionEntity> findTop100ByOrderByCreatedAtDesc();
}
