package com.lingfeng.sprite.skill;

import com.lingfeng.sprite.MemorySystem;
import com.lingfeng.sprite.SelfModel;
import com.lingfeng.sprite.WorldModel;

import java.time.Instant;
import java.util.Map;

/**
 * Context passed to a skill during execution
 */
public record SkillContext(
    String taskId,
    Map<String, Object> parameters,
    SelfModel.Self selfModel,
    WorldModel.World worldModel,
    MemorySystem.Memory memory,
    SkillContext previousContext,
    Instant startTime
) {
    /**
     * Create a new context with current timestamp
     */
    public static SkillContext create(
            String taskId,
            Map<String, Object> parameters,
            SelfModel.Self selfModel,
            WorldModel.World worldModel,
            MemorySystem.Memory memory
    ) {
        return new SkillContext(
            taskId,
            parameters,
            selfModel,
            worldModel,
            memory,
            null,
            Instant.now()
        );
    }

    /**
     * Create a child context (for chained skills)
     */
    public SkillContext child(Map<String, Object> additionalParams) {
        Map<String, Object> combined = new java.util.HashMap<>(parameters);
        combined.putAll(additionalParams);
        return new SkillContext(
            taskId,
            combined,
            selfModel,
            worldModel,
            memory,
            this,
            startTime
        );
    }

    /**
     * Get execution duration in milliseconds
     */
    public long durationMs() {
        return Instant.now().toEpochMilli() - startTime.toEpochMilli();
    }
}
