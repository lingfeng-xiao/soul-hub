package com.lingfeng.sprite.action;

/**
 * ActionStatus - Action execution status enum
 *
 * Represents the state machine for action execution:
 * - PENDING: Action is created but not yet started
 * - RUNNING: Action is currently executing
 * - SUCCEEDED: Action completed successfully
 * - FAILED: Action execution failed
 * - COMPENSATED: Failed action has been compensated (rolled back)
 */
public enum ActionStatus {

    /**
     * Action is created but not yet dispatched for execution
     */
    PENDING,

    /**
     * Action is currently being executed
     */
    RUNNING,

    /**
     * Action completed successfully
     */
    SUCCEEDED,

    /**
     * Action execution failed
     */
    FAILED,

    /**
     * Failed action has been compensated (rolled back)
     */
    COMPENSATED;

    /**
     * Check if the status represents a terminal state
     */
    public boolean isTerminal() {
        return this == SUCCEEDED || this == FAILED || this == COMPENSATED;
    }

    /**
     * Check if transition to the target status is valid
     */
    public boolean canTransitionTo(ActionStatus target) {
        return switch (this) {
            case PENDING -> target == RUNNING || target == FAILED;
            case RUNNING -> target == SUCCEEDED || target == FAILED;
            case FAILED -> target == COMPENSATED;
            case SUCCEEDED, COMPENSATED -> false; // Terminal states
        };
    }
}
