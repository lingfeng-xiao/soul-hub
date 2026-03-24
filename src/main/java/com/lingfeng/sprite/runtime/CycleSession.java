package com.lingfeng.sprite.runtime;

import java.time.Instant;

/**
 * CycleSession represents the state of a single cognitive cycle.
 * Each cycle progresses through distinct phases from creation to completion.
 */
public enum CycleSession {
    /** Cycle has been created but not yet started */
    CREATED,

    /** Cycle is in the data collection phase */
    COLLECTING,

    /** Cycle is performing reasoning */
    REASONING,

    /** Cycle is in decision making phase */
    DECIDING,

    /** Cycle is executing actions */
    ACTING,

    /** Cycle is consolidating results and memories */
    CONSOLIDATING,

    /** Cycle completed successfully */
    COMPLETED,

    /** Cycle failed during execution */
    FAILED,

    /** Cycle was cancelled before completion */
    CANCELLED
}
