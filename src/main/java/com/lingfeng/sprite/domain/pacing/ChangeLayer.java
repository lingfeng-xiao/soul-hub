package com.lingfeng.sprite.domain.pacing;

/**
 * ChangeLayer - 变化分层
 *
 * 用于标记变化的节速层级，决定变化的处理方式。
 *
 * 对应 IGN-091
 */
public enum ChangeLayer {
    /**
     * 快速层 - prompt, workflow, task strategy, research strategy
     * 可立即应用
     */
    FAST,

    /**
     * 中速层 - reminder style, collaboration rhythm, learning support, initiative strategy
     * 解释后应用
     */
    MEDIUM,

    /**
     * 慢速层 - identity narrative, personality style, relationship pattern, value tendency
     * 确认后应用
     */
    SLOW
}
