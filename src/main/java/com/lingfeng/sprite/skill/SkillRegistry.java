package com.lingfeng.sprite.skill;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

/**
 * Registry for managing skills
 * Handles loading, registration, and lookup of skills by trigger
 */
@Component
public class SkillRegistry {
    private static final Logger logger = LoggerFactory.getLogger(SkillRegistry.class);

    private final Map<String, Skill> skillsById = new ConcurrentHashMap<>();
    private final Map<String, List<Skill>> skillsByTrigger = new ConcurrentHashMap<>();
    private final SkillMarkdownParser parser = new SkillMarkdownParser();

    /**
     * Register a skill
     */
    public void register(Skill skill) {
        if (skill == null || skill.id() == null) {
            throw new IllegalArgumentException("Skill or skill id cannot be null");
        }

        skillsById.put(skill.id(), skill);

        // Index by triggers
        for (String trigger : skill.triggers()) {
            String normalized = normalizeTrigger(trigger);
            skillsByTrigger.computeIfAbsent(normalized, k -> new CopyOnWriteArrayList<>()).add(skill);
        }

        logger.info("Registered skill: {} (v{}) with {} triggers",
            skill.name(), skill.version(), skill.triggers().size());
    }

    /**
     * Unregister a skill
     */
    public void unregister(String skillId) {
        Skill skill = skillsById.remove(skillId);
        if (skill != null) {
            // Remove from trigger indexes
            for (String trigger : skill.triggers()) {
                String normalized = normalizeTrigger(trigger);
                List<Skill> list = skillsByTrigger.get(normalized);
                if (list != null) {
                    list.remove(skill);
                    if (list.isEmpty()) {
                        skillsByTrigger.remove(normalized);
                    }
                }
            }
            logger.info("Unregistered skill: {}", skillId);
        }
    }

    /**
     * Get a skill by ID
     */
    public Optional<Skill> get(String skillId) {
        return Optional.ofNullable(skillsById.get(skillId));
    }

    /**
     * Find skills matching a trigger phrase
     */
    public List<Skill> findByTrigger(String triggerPhrase) {
        if (triggerPhrase == null || triggerPhrase.isBlank()) {
            return List.of();
        }

        String normalized = normalizeTrigger(triggerPhrase);
        List<Skill> matches = new ArrayList<>();

        // Exact match
        List<Skill> exact = skillsByTrigger.get(normalized);
        if (exact != null) {
            matches.addAll(exact);
        }

        // Substring match
        for (Map.Entry<String, List<Skill>> entry : skillsByTrigger.entrySet()) {
            if (!entry.getKey().equals(normalized) &&
                normalized.contains(entry.getKey())) {
                for (Skill skill : entry.getValue()) {
                    if (!matches.contains(skill)) {
                        matches.add(skill);
                    }
                }
            }
        }

        return matches;
    }

    /**
     * Find the best matching skill for a trigger phrase
     */
    public Optional<Skill> findBestMatch(String triggerPhrase) {
        List<Skill> matches = findByTrigger(triggerPhrase);
        if (matches.isEmpty()) {
            return Optional.empty();
        }
        // Return the first one (could be improved with scoring)
        return Optional.of(matches.get(0));
    }

    /**
     * Load a skill from a SKILL.md content string
     */
    public Skill loadFromMarkdown(String skillId, String markdown) {
        Skill skill = parser.parse(skillId, markdown);
        register(skill);
        return skill;
    }

    /**
     * Get all registered skills
     */
    public Collection<Skill> getAll() {
        return List.copyOf(skillsById.values());
    }

    /**
     * Get skill count
     */
    public int count() {
        return skillsById.size();
    }

    /**
     * Check if a skill is registered
     */
    public boolean isRegistered(String skillId) {
        return skillsById.containsKey(skillId);
    }

    private String normalizeTrigger(String trigger) {
        return trigger.toLowerCase().trim();
    }
}
