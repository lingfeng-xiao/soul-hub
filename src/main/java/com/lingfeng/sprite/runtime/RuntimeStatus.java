package com.lingfeng.sprite.runtime;

/**
 * Runtime status enum representing the lifecycle states of the RuntimeCoordinator.
 */
public enum RuntimeStatus {
    /** Runtime is actively executing cycles */
    RUNNING,

    /** Runtime is paused and not executing new cycles */
    PAUSED,

    /** Runtime has been stopped and cannot resume */
    STOPPED
}
