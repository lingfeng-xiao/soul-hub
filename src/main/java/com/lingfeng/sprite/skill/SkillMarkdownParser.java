package com.lingfeng.sprite.skill;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parser for SKILL.md format
 * Extracts skill metadata, triggers, parameters from markdown content
 */
public class SkillMarkdownParser {

    private static final Pattern ID_PATTERN = Pattern.compile("^id:\\s*(.+)$", Pattern.MULTILINE);
    private static final Pattern NAME_PATTERN = Pattern.compile("^name:\\s*(.+)$", Pattern.MULTILINE);
    private static final Pattern VERSION_PATTERN = Pattern.compile("^version:\\s*(.+)$", Pattern.MULTILINE);
    private static final Pattern AUTHOR_PATTERN = Pattern.compile("^author:\\s*(.+)$", Pattern.MULTILINE);
    private static final Pattern DESC_PATTERN = Pattern.compile("^description:\\s*(.+)$", Pattern.MULTILINE);
    private static final Pattern TRIGGER_PATTERN = Pattern.compile("-\\s*[\"\"]?([^\"\n]+)[\"\"]?", Pattern.MULTILINE);
    private static final Pattern PARAM_PATTERN = Pattern.compile("\\|\\s*(\\w+)\\s*\\|\\s*(\\w+)\\s*\\|\\s*(yes|no)\\s*\\|\\s*(.+?)\\s*\\|", Pattern.MULTILINE);

    /**
     * Parse SKILL.md content into a Skill
     */
    public Skill parse(String skillId, String markdown) {
        String name = extract(NAME_PATTERN, markdown, skillId);
        String version = extract(VERSION_PATTERN, markdown, "1.0.0");
        String description = extract(DESC_PATTERN, markdown, "");
        String author = extract(AUTHOR_PATTERN, markdown, "unknown");

        List<String> triggers = extractTriggers(markdown);
        List<Skill.SkillParameter> parameters = extractParameters(markdown);

        return new ParsedSkill(
            skillId,
            name,
            description,
            version,
            author,
            triggers,
            parameters
        );
    }

    private String extract(Pattern pattern, String markdown, String defaultValue) {
        Matcher matcher = pattern.matcher(markdown);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        return defaultValue;
    }

    private List<String> extractTriggers(String markdown) {
        List<String> triggers = new ArrayList<>();

        // Find ## Triggers section
        int triggersStart = markdown.indexOf("## Triggers");
        if (triggersStart == -1) {
            triggersStart = markdown.indexOf("## triggers");
        }

        if (triggersStart != -1) {
            // Find next section or end
            int triggersEnd = markdown.indexOf("##", triggersStart + 2);
            String triggersSection = triggersEnd == -1
                ? markdown.substring(triggersStart)
                : markdown.substring(triggersStart, triggersEnd);

            Matcher matcher = TRIGGER_PATTERN.matcher(triggersSection);
            while (matcher.find()) {
                String trigger = matcher.group(1).trim();
                if (!trigger.isEmpty()) {
                    triggers.add(trigger);
                }
            }
        }

        return triggers;
    }

    private List<Skill.SkillParameter> extractParameters(String markdown) {
        List<Skill.SkillParameter> params = new ArrayList<>();

        // Find ## Parameters section
        int paramsStart = markdown.indexOf("## Parameters");
        if (paramsStart == -1) {
            paramsStart = markdown.indexOf("## parameters");
        }

        if (paramsStart != -1) {
            // Find next section or end
            int paramsEnd = markdown.indexOf("##", paramsStart + 2);
            String paramsSection = paramsEnd == -1
                ? markdown.substring(paramsStart)
                : markdown.substring(paramsStart, paramsEnd);

            Matcher matcher = PARAM_PATTERN.matcher(paramsSection);
            while (matcher.find()) {
                String paramName = matcher.group(1).trim();
                String paramType = matcher.group(2).trim();
                boolean required = "yes".equalsIgnoreCase(matcher.group(3).trim());
                String paramDesc = matcher.group(4).trim();

                params.add(new Skill.SkillParameter(
                    paramName, paramType, required, paramDesc, null
                ));
            }
        }

        return params;
    }

    /**
     * Simple implementation of Skill for parsed markdown
     */
    private static class ParsedSkill implements Skill {
        private final String id;
        private final String name;
        private final String description;
        private final String version;
        private final String author;
        private final List<String> triggers;
        private final List<SkillParameter> parameters;

        public ParsedSkill(String id, String name, String description, String version,
                          String author, List<String> triggers, List<SkillParameter> parameters) {
            this.id = id;
            this.name = name;
            this.description = description;
            this.version = version;
            this.author = author;
            this.triggers = List.copyOf(triggers);
            this.parameters = List.copyOf(parameters);
        }

        @Override
        public String id() { return id; }

        @Override
        public String name() { return name; }

        @Override
        public String description() { return description; }

        @Override
        public String version() { return version; }

        @Override
        public List<String> triggers() { return triggers; }

        @Override
        public List<SkillParameter> parameters() { return parameters; }

        @Override
        public SkillResult execute(SkillContext context) {
            // This is a parsed skill - execution would be handled by a real implementation
            return SkillResult.failure("Parsed skill cannot be executed directly: " + id);
        }
    }
}
