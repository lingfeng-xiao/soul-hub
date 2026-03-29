package com.lingfeng.sprite.agent;

import com.lingfeng.sprite.MemorySystem;
import com.lingfeng.sprite.SelfModel;
import com.lingfeng.sprite.WorldModel;
import com.lingfeng.sprite.skill.Skill;
import com.lingfeng.sprite.skill.SkillContext;
import com.lingfeng.sprite.skill.SkillRegistry;
import com.lingfeng.sprite.skill.SkillResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;

/**
 * Executes skills with proper context for agent integration.
 * Provides a clean interface for workers to invoke skills during cognitive cycles.
 */
@Component
public class SkillExecutor {
    private static final Logger logger = LoggerFactory.getLogger(SkillExecutor.class);

    private final SkillRegistry registry;

    public SkillExecutor(SkillRegistry registry) {
        this.registry = registry;
    }

    /**
     * Execute a skill by ID with full context
     */
    public SkillResult executeSkill(String skillId, Map<String, Object> parameters,
                                    SelfModel.Self selfModel, WorldModel.World worldModel,
                                    MemorySystem.Memory memory) {
        return executeSkill(skillId, "agent-" + System.currentTimeMillis(), parameters, selfModel, worldModel, memory);
    }

    /**
     * Execute a skill by ID with full context and custom task ID
     */
    public SkillResult executeSkill(String skillId, String taskId, Map<String, Object> parameters,
                                    SelfModel.Self selfModel, WorldModel.World worldModel,
                                    MemorySystem.Memory memory) {
        Optional<Skill> skillOpt = registry.get(skillId);
        if (skillOpt.isEmpty()) {
            logger.warn("Skill not found: {}", skillId);
            return SkillResult.failure("Skill not found: " + skillId);
        }

        return execute(skillOpt.get(), taskId, parameters, selfModel, worldModel, memory);
    }

    /**
     * Execute a skill by trigger phrase with full context
     */
    public SkillResult executeByTrigger(String trigger, Map<String, Object> parameters,
                                        SelfModel.Self selfModel, WorldModel.World worldModel,
                                        MemorySystem.Memory memory) {
        return executeByTrigger(trigger, "agent-" + System.currentTimeMillis(), parameters, selfModel, worldModel, memory);
    }

    /**
     * Execute a skill by trigger phrase with full context and custom task ID
     */
    public SkillResult executeByTrigger(String trigger, String taskId, Map<String, Object> parameters,
                                         SelfModel.Self selfModel, WorldModel.World worldModel,
                                         MemorySystem.Memory memory) {
        Optional<Skill> skillOpt = registry.findBestMatch(trigger);
        if (skillOpt.isEmpty()) {
            logger.warn("No skill found for trigger: {}", trigger);
            return SkillResult.failure("No skill found for trigger: " + trigger);
        }

        return execute(skillOpt.get(), taskId, parameters, selfModel, worldModel, memory);
    }

    /**
     * Check if a skill is registered
     */
    public boolean isSkillRegistered(String skillId) {
        return registry.isRegistered(skillId);
    }

    /**
     * Get all available skill IDs
     */
    public java.util.List<String> getAvailableSkillIds() {
        return registry.getAll().stream().map(Skill::id).toList();
    }

    private SkillResult execute(Skill skill, String taskId, Map<String, Object> parameters,
                               SelfModel.Self selfModel, WorldModel.World worldModel,
                               MemorySystem.Memory memory) {
        long startTime = System.currentTimeMillis();

        try {
            logger.info("Executing skill: {} (taskId={})", skill.id(), taskId);

            SkillContext context = SkillContext.create(taskId, parameters, selfModel, worldModel, memory);
            SkillResult result = skill.execute(context);

            long duration = System.currentTimeMillis() - startTime;
            logger.info("Skill {} executed in {}ms, success={}", skill.id(), duration, result.success());

            return result.withDuration(duration);
        } catch (Exception e) {
            logger.error("Error executing skill {}: {}", skill.id(), e.getMessage(), e);
            return SkillResult.failure("Execution error: " + e.getMessage());
        }
    }
}
