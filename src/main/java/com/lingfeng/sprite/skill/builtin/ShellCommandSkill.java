package com.lingfeng.sprite.skill.builtin;

import com.lingfeng.sprite.skill.Skill;
import com.lingfeng.sprite.skill.SkillContext;
import com.lingfeng.sprite.skill.SkillResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

/**
 * Skill for executing shell commands
 */
public class ShellCommandSkill implements Skill {
    private static final Logger logger = LoggerFactory.getLogger(ShellCommandSkill.class);

    // Block dangerous commands
    private static final List<Pattern> BLOCKED_PATTERNS = List.of(
        Pattern.compile("rm\\s+-rf\\s+/"),
        Pattern.compile("fork\\s+bomb", Pattern.CASE_INSENSITIVE),
        Pattern.compile(":\\(\\)\\{\\s*:\\|:\\s*\\};:", Pattern.CASE_INSENSITIVE),
        Pattern.compile(">\\s*/dev/sda"),
        Pattern.compile("dd\\s+if=.*of=/dev/sd")
    );

    private static final long DEFAULT_TIMEOUT_MS = 30000;
    private static final long MAX_TIMEOUT_MS = 300000;

    @Override
    public String id() {
        return "shell-command-v1";
    }

    @Override
    public String name() {
        return "Shell Commands";
    }

    @Override
    public String description() {
        return "Execute shell commands and return output";
    }

    @Override
    public String version() {
        return "1.0.0";
    }

    @Override
    public List<String> triggers() {
        return List.of("run command", "execute", "shell", "terminal", "bash", "cmd");
    }

    @Override
    public List<SkillParameter> parameters() {
        return List.of(
            new SkillParameter("command", "string", true, "Shell command to execute", null),
            new SkillParameter("timeout", "integer", false, "Timeout in milliseconds", "30000"),
            new SkillParameter("workingDir", "string", false, "Working directory", System.getProperty("user.dir"))
        );
    }

    @Override
    public SkillResult execute(SkillContext context) {
        long start = System.currentTimeMillis();

        String command = (String) context.parameters().get("command");
        if (command == null || command.isBlank()) {
            return SkillResult.failure("Command is required");
        }

        // Security check
        if (isBlocked(command)) {
            logger.warn("Blocked dangerous command: {}", command);
            return SkillResult.failure("Command blocked for security reasons");
        }

        // Parse timeout
        long timeout = DEFAULT_TIMEOUT_MS;
        Object timeoutParam = context.parameters().get("timeout");
        if (timeoutParam instanceof Number) {
            timeout = Math.min(((Number) timeoutParam).longValue(), MAX_TIMEOUT_MS);
        } else if (timeoutParam instanceof String) {
            try {
                timeout = Math.min(Long.parseLong((String) timeoutParam), MAX_TIMEOUT_MS);
            } catch (NumberFormatException ignored) {}
        }

        // Parse working directory
        String workingDir = System.getProperty("user.dir");
        Object dirParam = context.parameters().get("workingDir");
        if (dirParam instanceof String && !((String) dirParam).isBlank()) {
            workingDir = (String) dirParam;
        }

        try {
            ProcessBuilder pb = new ProcessBuilder();
            // Cross-platform command handling
            if (System.getProperty("os.name").toLowerCase().contains("windows")) {
                pb.command("cmd", "/c", command);
            } else {
                pb.command("bash", "-c", command);
            }
            pb.directory(new java.io.File(workingDir));
            pb.redirectErrorStream(true);

            Process process = pb.start();

            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                }
            }

            boolean completed = process.waitFor(timeout, TimeUnit.MILLISECONDS);
            int exitCode = completed ? process.exitValue() : -1;

            long duration = System.currentTimeMillis() - start;

            if (!completed) {
                process.destroyForcibly();
                return SkillResult.failure("Command timed out after " + timeout + "ms")
                    .withDuration(duration)
                    .withMetadata("timeout", "true");
            }

            if (exitCode == 0) {
                return SkillResult.success("Command executed successfully", output.toString())
                    .withDuration(duration)
                    .withMetadata("exitCode", "0");
            } else {
                return SkillResult.success("Command completed with exit code " + exitCode, output.toString())
                    .withDuration(duration)
                    .withMetadata("exitCode", String.valueOf(exitCode));
            }

        } catch (Exception e) {
            logger.error("Error executing command: {}", command, e);
            long duration = System.currentTimeMillis() - start;
            return SkillResult.failure("Error executing command: " + e.getMessage())
                .withDuration(duration);
        }
    }

    private boolean isBlocked(String command) {
        for (Pattern pattern : BLOCKED_PATTERNS) {
            if (pattern.matcher(command).find()) {
                return true;
            }
        }
        return false;
    }
}
