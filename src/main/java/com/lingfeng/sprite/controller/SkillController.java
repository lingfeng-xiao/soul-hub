package com.lingfeng.sprite.controller;

import com.lingfeng.sprite.skill.Skill;
import com.lingfeng.sprite.skill.SkillRegistry;
import com.lingfeng.sprite.skill.SkillContext;
import com.lingfeng.sprite.skill.SkillResult;
import com.lingfeng.sprite.skill.builtin.ShellCommandSkill;
import com.lingfeng.sprite.skill.builtin.FileOperationSkill;
import com.lingfeng.sprite.skill.builtin.HttpRequestSkill;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * REST controller for skill management
 */
@RestController
@RequestMapping("/api/skill")
public class SkillController {
    private static final Logger logger = LoggerFactory.getLogger(SkillController.class);

    private final SkillRegistry registry;

    public SkillController(SkillRegistry registry) {
        this.registry = registry;
        // Register built-in skills
        registry.register(new ShellCommandSkill());
        registry.register(new FileOperationSkill());
        registry.register(new HttpRequestSkill());
    }

    /**
     * Get all registered skills
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> getSkills() {
        List<Map<String, Object>> skills = registry.getAll().stream()
            .map(this::skillToMap)
            .collect(Collectors.toList());

        Map<String, Object> response = new HashMap<>();
        response.put("skills", skills);
        response.put("count", skills.size());
        return ResponseEntity.ok(response);
    }

    /**
     * Get a specific skill by ID
     */
    @GetMapping("/{skillId}")
    public ResponseEntity<Skill> getSkill(@PathVariable("skillId") String skillId) {
        return registry.get(skillId)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Find skills by trigger
     */
    @GetMapping("/find")
    public ResponseEntity<Map<String, Object>> findByTrigger(@RequestParam String trigger) {
        List<Skill> matches = registry.findByTrigger(trigger);
        Map<String, Object> response = new HashMap<>();
        response.put("trigger", trigger);
        response.put("matches", matches.stream().map(Skill::id).toList());
        response.put("count", matches.size());
        return ResponseEntity.ok(response);
    }

    /**
     * Execute a skill by ID
     */
    @PostMapping("/{skillId}/execute")
    public ResponseEntity<Map<String, Object>> executeSkill(
            @PathVariable("skillId") String skillId,
            @RequestBody(required = false) Map<String, Object> parameters
    ) {
        Skill skill = registry.get(skillId).orElse(null);
        if (skill == null) {
            return ResponseEntity.notFound().build();
        }

        try {
            if (parameters == null) {
                parameters = new HashMap<>();
            }
            SkillContext context = SkillContext.create(
                "cmd-" + System.currentTimeMillis(),
                parameters,
                null, null, null
            );
            SkillResult result = skill.execute(context);

            Map<String, Object> response = new HashMap<>();
            response.put("success", result.success());
            response.put("message", result.message());
            response.put("data", result.data());
            response.put("durationMs", result.durationMs());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error executing skill {}: {}", skillId, e.getMessage(), e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", e.getClass().getSimpleName() + ": " + e.getMessage());
            errorResponse.put("message", "Skill execution failed: " + e.getMessage());
            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    /**
     * Execute a skill by trigger phrase
     */
    @PostMapping("/execute")
    public ResponseEntity<Map<String, Object>> executeByTrigger(
            @RequestParam String trigger,
            @RequestBody(required = false) Map<String, Object> parameters
    ) {
        final Map<String, Object> params = parameters != null ? parameters : new HashMap<>();
        return registry.findBestMatch(trigger)
            .map(skill -> {
                try {
                    SkillContext context = SkillContext.create(
                        "cmd-" + System.currentTimeMillis(),
                        params,
                        null, null, null
                    );
                    SkillResult result = skill.execute(context);

                    Map<String, Object> response = new HashMap<>();
                    response.put("matchedSkill", skill.id());
                    response.put("success", result.success());
                    response.put("message", result.message());
                    response.put("data", result.data());
                    response.put("durationMs", result.durationMs());
                    return ResponseEntity.ok(response);
                } catch (Exception e) {
                    logger.error("Error executing skill by trigger {}: {}", trigger, e.getMessage(), e);
                    Map<String, Object> errorResponse = new HashMap<>();
                    errorResponse.put("success", false);
                    errorResponse.put("error", e.getClass().getSimpleName() + ": " + e.getMessage());
                    errorResponse.put("message", "Skill execution failed: " + e.getMessage());
                    return ResponseEntity.status(500).body(errorResponse);
                }
            })
            .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Create a skill from markdown content
     */
    @PostMapping
    public ResponseEntity<Map<String, Object>> createSkill(
            @RequestParam String skillId,
            @RequestBody String markdown
    ) {
        try {
            Skill skill = registry.loadFromMarkdown(skillId, markdown);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("skill", skillToMap(skill));
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error creating skill from markdown: {}", e.getMessage(), e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", e.getClass().getSimpleName() + ": " + e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    /**
     * Delete/unregister a skill
     */
    @DeleteMapping("/{skillId}")
    public ResponseEntity<Map<String, Object>> deleteSkill(@PathVariable("skillId") String skillId) {
        if (!registry.isRegistered(skillId)) {
            return ResponseEntity.notFound().build();
        }

        registry.unregister(skillId);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("skillId", skillId);
        response.put("message", "Skill unregistered successfully");
        return ResponseEntity.ok(response);
    }

    private Map<String, Object> skillToMap(Skill skill) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", skill.id());
        map.put("name", skill.name());
        map.put("description", skill.description());
        map.put("version", skill.version());
        map.put("triggers", skill.triggers());
        map.put("parameters", skill.parameters());
        map.put("async", skill.isAsync());
        return map;
    }
}
