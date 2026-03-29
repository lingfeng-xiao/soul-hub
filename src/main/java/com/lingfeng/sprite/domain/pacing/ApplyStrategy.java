package com.lingfeng.sprite.domain.pacing;

/**
 * ApplyStrategy - 应用策略
 *
 * 决定变化如何应用到系统中。
 *
 * 对应 IGN-090
 */
public enum ApplyStrategy {
    /**
     * 立即应用 - 快速层变化直接应用
     */
    IMMEDIATE,

    /**
     * 解释后应用 - 中速层变化需要先解释给主人
     */
    EXPLAIN_THEN_APPLY,

    /**
     * 确认后应用 - 慢速层变化需要主人明确确认
     */
    CONFIRM_THEN_APPLY
}
