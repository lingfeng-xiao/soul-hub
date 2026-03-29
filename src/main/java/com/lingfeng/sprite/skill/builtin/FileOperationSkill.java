package com.lingfeng.sprite.skill.builtin;

import com.lingfeng.sprite.skill.Skill;
import com.lingfeng.sprite.skill.SkillContext;
import com.lingfeng.sprite.skill.SkillResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;

/**
 * Skill for file operations (read, write, delete, list, search)
 */
public class FileOperationSkill implements Skill {
    private static final Logger logger = LoggerFactory.getLogger(FileOperationSkill.class);

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(
        "txt", "md", "json", "xml", "yaml", "yml", "properties",
        "java", "py", "js", "ts", "html", "css", "sql",
        "log", "csv", "ini", "conf", "sh", "bat", "ps1"
    );

    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024; // 10MB

    @Override
    public String id() {
        return "file-operation-v1";
    }

    @Override
    public String name() {
        return "File Operations";
    }

    @Override
    public String description() {
        return "Read, write, delete, and search files";
    }

    @Override
    public String version() {
        return "1.0.0";
    }

    @Override
    public List<String> triggers() {
        return List.of("read file", "write file", "delete file", "list files", "search files", "file");
    }

    @Override
    public List<SkillParameter> parameters() {
        return List.of(
            new SkillParameter("operation", "enum", true, "read|write|delete|list|search", null),
            new SkillParameter("path", "string", true, "File or directory path", null),
            new SkillParameter("content", "string", false, "Content for write operation", null),
            new SkillParameter("pattern", "string", false, "Glob pattern for search", "*"),
            new SkillParameter("recursive", "boolean", false, "Recursive for directory operations", "false")
        );
    }

    @Override
    public SkillResult execute(SkillContext context) {
        long start = System.currentTimeMillis();

        String operation = (String) context.parameters().get("operation");
        String path = (String) context.parameters().get("path");

        if (operation == null || operation.isBlank()) {
            return SkillResult.failure("Operation is required (read|write|delete|list|search)");
        }

        if (path == null || path.isBlank()) {
            return SkillResult.failure("Path is required");
        }

        try {
            return switch (operation.toLowerCase()) {
                case "read" -> readFile(path, start);
                case "write" -> writeFile(path, context.parameters().get("content"), start);
                case "delete" -> deleteFile(path, start);
                case "list" -> listDirectory(path, context.parameters().get("pattern"),
                    (Boolean) context.parameters().getOrDefault("recursive", false), start);
                case "search" -> searchFiles(path, (String) context.parameters().get("pattern"), start);
                default -> SkillResult.failure("Unknown operation: " + operation);
            };
        } catch (Exception e) {
            logger.error("Error performing file operation {} on {}", operation, path, e);
            long duration = System.currentTimeMillis() - start;
            return SkillResult.failure("Error: " + e.getMessage()).withDuration(duration);
        }
    }

    private SkillResult readFile(String path, long startTime) throws IOException {
        Path filePath = Paths.get(path).toAbsolutePath();

        if (!Files.exists(filePath)) {
            return SkillResult.failure("File not found: " + path);
        }

        if (!Files.isRegularFile(filePath)) {
            return SkillResult.failure("Not a file: " + path);
        }

        if (Files.size(filePath) > MAX_FILE_SIZE) {
            return SkillResult.failure("File too large (max 10MB): " + path);
        }

        String content = Files.readString(filePath);
        long duration = System.currentTimeMillis() - startTime;

        return SkillResult.success("File read successfully", Map.of(
            "path", path,
            "size", Files.size(filePath),
            "lastModified", Instant.ofEpochMilli(Files.getLastModifiedTime(filePath).toMillis()).toString(),
            "content", content.length() > 1000 ? content.substring(0, 1000) + "..." : content
        )).withDuration(duration);
    }

    private SkillResult writeFile(String path, Object content, long startTime) throws IOException {
        if (content == null) {
            return SkillResult.failure("Content is required for write operation");
        }

        Path filePath = Paths.get(path).toAbsolutePath();

        // Create parent directories if needed
        Path parent = filePath.getParent();
        if (parent != null && !Files.exists(parent)) {
            Files.createDirectories(parent);
        }

        String contentStr = content.toString();
        Files.writeString(filePath, contentStr);

        long duration = System.currentTimeMillis() - startTime;

        return SkillResult.success("File written successfully", Map.of(
            "path", path,
            "bytesWritten", contentStr.length()
        )).withDuration(duration);
    }

    private SkillResult deleteFile(String path, long startTime) throws IOException {
        Path filePath = Paths.get(path).toAbsolutePath();

        if (!Files.exists(filePath)) {
            return SkillResult.failure("File not found: " + path);
        }

        // Move to .trash instead of permanent delete
        Path trashDir = Paths.get(System.getProperty("user.home"), ".sprites", ".trash");
        Files.createDirectories(trashDir);

        Path trashPath = trashDir.resolve(filePath.getFileName());
        Files.move(filePath, trashPath, StandardCopyOption.REPLACE_EXISTING);

        long duration = System.currentTimeMillis() - startTime;

        return SkillResult.success("File moved to trash", Map.of(
            "originalPath", path,
            "trashPath", trashPath.toString()
        )).withDuration(duration);
    }

    private SkillResult listDirectory(String path, Object pattern, boolean recursive, long startTime) throws IOException {
        Path dirPath = Paths.get(path).toAbsolutePath();

        if (!Files.exists(dirPath)) {
            return SkillResult.failure("Directory not found: " + path);
        }

        if (!Files.isDirectory(dirPath)) {
            return SkillResult.failure("Not a directory: " + path);
        }

        String globPattern = (pattern instanceof String) ? (String) pattern : "*";
        List<Map<String, String>> entries = new ArrayList<>();

        if (recursive) {
            PathMatcher matcher = FileSystems.getDefault().getPathMatcher("glob:**/" + globPattern);
            Files.walk(dirPath)
                .filter(matcher::matches)
                .limit(1000)
                .forEach(p -> entries.add(entryInfo(dirPath, p)));
        } else {
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(dirPath, globPattern)) {
                for (Path p : stream) {
                    entries.add(entryInfo(dirPath, p));
                    if (entries.size() >= 100) break;
                }
            }
        }

        long duration = System.currentTimeMillis() - startTime;

        return SkillResult.success("Directory listed", Map.of(
            "path", path,
            "entryCount", entries.size(),
            "entries", entries
        )).withDuration(duration);
    }

    private Map<String, String> entryInfo(Path base, Path entry) {
        try {
            return Map.of(
                "name", entry.getFileName().toString(),
                "type", Files.isDirectory(entry) ? "directory" : "file",
                "size", String.valueOf(Files.isDirectory(entry) ? 0 : Files.size(entry))
            );
        } catch (IOException e) {
            return Map.of("name", entry.getFileName().toString(), "type", "unknown", "size", "0");
        }
    }

    private SkillResult searchFiles(String path, String pattern, long startTime) throws IOException {
        Path searchPath = Paths.get(path).toAbsolutePath();

        if (!Files.exists(searchPath)) {
            return SkillResult.failure("Search path not found: " + path);
        }

        String globPattern = pattern != null ? pattern : "*";
        List<String> matches = new ArrayList<>();

        PathMatcher matcher = FileSystems.getDefault().getPathMatcher("glob:**/" + globPattern);
        Files.walk(searchPath)
            .filter(matcher::matches)
            .filter(Files::isRegularFile)
            .limit(100)
            .forEach(p -> matches.add(searchPath.relativize(p).toString()));

        long duration = System.currentTimeMillis() - startTime;

        return SkillResult.success("Search completed", Map.of(
            "pattern", globPattern,
            "matches", matches,
            "count", matches.size()
        )).withDuration(duration);
    }
}
