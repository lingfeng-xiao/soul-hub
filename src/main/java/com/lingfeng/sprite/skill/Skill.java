package com.lingfeng.sprite.skill;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Skill interface - defines a capability that can be executed by the sprite
 */
public interface Skill {

    /**
     * Unique identifier for this skill
     */
    String id();

    /**
     * Human-readable name
     */
    String name();

    /**
     * Description of what this skill does
     */
    String description();

    /**
     * Version string
     */
    String version();

    /**
     * Keywords/triggers that activate this skill
     */
    List<String> triggers();

    /**
     * Parameters this skill accepts
     */
    List<SkillParameter> parameters();

    /**
     * Execute the skill
     */
    SkillResult execute(SkillContext context);

    /**
     * Whether this skill executes asynchronously
     */
    default boolean isAsync() {
        return false;
    }

    /**
     * Skill parameter definition
     */
    record SkillParameter(
        String name,
        String type,
        boolean required,
        String description,
        String defaultValue
    ) {
        public SkillParameter {
            if (type == null) type = "string";
            if (required) required = true;
        }
    }

    /**
     * Metadata about a skill
     */
    record SkillMetadata(
        String id,
        String name,
        String version,
        String author,
        String description,
        List<String> tags,
        Instant createdAt
    ) {}
}
