package com.lingfeng.sprite.domain.memory.life;

import java.time.Instant;

/**
 * LifeMemory - 生命型记忆接口
 *
 * 所有生命塑造型记忆的基类接口。
 * 这些记忆会塑造 Sprite 的当前行为和表达。
 *
 * 对应 IGN-030
 */
public interface LifeMemory {

    /**
     * 获取记忆 ID
     */
    String getMemoryId();

    /**
     * 获取记忆内容
     */
    String getContent();

    /**
     * 获取记忆发生时间
     */
    Instant getOccurredAt();

    /**
     * 获取意义描述 - 为什么这个记忆塑造当前行为
     */
    String getSignificance();

    /**
     * 检查这个记忆是否仍然与当前相关
     */
    boolean isStillRelevant();

    /**
     * 更新相关性
     */
    LifeMemory withRelevance(boolean relevant);
}
