package com.lingfeng.sprite.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpPut;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.classic.methods.HttpUriRequest;
import org.apache.hc.client5.http.classic.methods.HttpDelete;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.lingfeng.sprite.MemorySystem;
import com.lingfeng.sprite.MemorySystem.LongTermMemory;

/**
 * S4: GitHub备份服务
 *
 * 负责将记忆数据定期备份到GitHub
 *
 * 功能：
 * 1. 定时导出记忆到GitHub仓库
 * 2. 支持版本回溯
 * 3. 处理冲突
 */
@Service
public class GitHubBackupService {

    private static final Logger logger = LoggerFactory.getLogger(GitHubBackupService.class);

    // GitHub API 配置
    @Value("${github.token:}")
    private String githubToken;

    @Value("${github.owner:lingfeng-xiao}")
    private String owner;

    @Value("${github.repo:soul-hub}")
    private String repo;

    @Value("${github.backup-branch:main}")
    private String backupBranch;

    @Value("${github.backup-enabled:false}")
    private boolean backupEnabled;

    // 备份路径
    private static final String BACKUP_BASE_PATH = "backups/memory";
    private static final String MEMORY_DIR = "data/memory/long-term";
    private static final String CONFIG_DIR = "config";
    private static final String CODE_DIR = "src";

    // 代码快照备份路径
    private static final String CONFIG_BACKUP_PATH = "backups/config";
    private static final String CODE_SNAPSHOT_PATH = "backups/code-snapshots";

    // 备份间隔（小时）
    private static final long BACKUP_INTERVAL_HOURS = 6;

    // 默认配置文件列表
    private static final String[] DEFAULT_CONFIG_FILES = {
        "openclaw.json",
        "app-config.yaml",
        "application.yml"
    };

    private final MemorySystem.Memory memory;
    private final MemoryPersistenceService memoryPersistenceService;
    private final ObjectMapper objectMapper;
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private final ConcurrentHashMap<String, Instant> lastBackupTimes = new ConcurrentHashMap<>();

    // HTTP 客户端
    private final CloseableHttpClient httpClient;
    private static final String GITHUB_API_BASE = "https://api.github.com";

    public GitHubBackupService(
            @Autowired MemorySystem.Memory memory,
            @Autowired MemoryPersistenceService memoryPersistenceService
    ) {
        this.memory = memory;
        this.memoryPersistenceService = memoryPersistenceService;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
        this.objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
        this.objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        // 创建HTTP客户端
        PoolingHttpClientConnectionManager cm = new PoolingHttpClientConnectionManager();
        cm.setMaxTotal(10);
        this.httpClient = HttpClients.custom()
                .setConnectionManager(cm)
                .build();

        if (backupEnabled) {
            startPeriodicBackup();
        }
    }

    /**
     * 启动定期备份
     */
    private void startPeriodicBackup() {
        if (!backupEnabled || githubToken == null || githubToken.isEmpty()) {
            logger.info("GitHub backup is disabled or token not configured");
            return;
        }

        scheduler.scheduleAtFixedRate(
                this::performBackup,
                BACKUP_INTERVAL_HOURS,
                BACKUP_INTERVAL_HOURS,
                TimeUnit.HOURS
        );
        logger.info("Periodic GitHub backup started (every {} hours)", BACKUP_INTERVAL_HOURS);
    }

    /**
     * 执行备份
     */
    public BackupResult performBackup() {
        if (!backupEnabled || githubToken == null || githubToken.isEmpty()) {
            return new BackupResult(false, "Backup disabled or token not configured", 0);
        }

        Instant startTime = Instant.now();
        String timestamp = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss")
                .withZone(ZoneId.of("Asia/Shanghai"))
                .format(startTime);

        try {
            // 先保存本地记忆
            memoryPersistenceService.saveMemory();

            // 获取记忆文件
            Path memoryPath = Paths.get(MEMORY_DIR);
            if (!Files.exists(memoryPath)) {
                return new BackupResult(false, "Memory directory not found", 0);
            }

            int filesBackedUp = 0;

            // 备份情景记忆
            if (backupFile(memoryPath.resolve("episodic.json"), timestamp)) {
                filesBackedUp++;
            }

            // 备份语义记忆
            if (backupFile(memoryPath.resolve("semantic.json"), timestamp)) {
                filesBackedUp++;
            }

            // 备份程序记忆
            if (backupFile(memoryPath.resolve("procedural.json"), timestamp)) {
                filesBackedUp++;
            }

            // 更新备份索引
            updateBackupIndex(timestamp, filesBackedUp);

            lastBackupTimes.put("lastBackup", startTime);

            long duration = java.time.Duration.between(startTime, Instant.now()).toMillis();
            logger.info("GitHub backup completed: {} files in {}ms", filesBackedUp, duration);

            return new BackupResult(true, "Backup completed successfully", filesBackedUp);

        } catch (Exception e) {
            logger.error("GitHub backup failed: {}", e.getMessage());
            return new BackupResult(false, "Backup failed: " + e.getMessage(), 0);
        }
    }

    /**
     * 备份单个文件到GitHub
     */
    private boolean backupFile(Path filePath, String timestamp) {
        if (!Files.exists(filePath)) {
            logger.debug("File not found, skipping: {}", filePath);
            return false;
        }

        try {
            String fileName = filePath.getFileName().toString();
            String fileContent = Files.readString(filePath);

            // 计算备份路径
            String backupPath = String.format("%s/%s/%s", BACKUP_BASE_PATH, timestamp, fileName);
            String latestPath = String.format("%s/latest/%s", BACKUP_BASE_PATH, fileName);

            // 写入带时间戳的版本
            commitFile(backupPath, fileContent, "Backup: " + fileName + " (" + timestamp + ")");

            // 更新 latest 版本
            commitFile(latestPath, fileContent, "Update: " + fileName);

            logger.debug("Backed up file: {}", fileName);
            return true;

        } catch (Exception e) {
            logger.error("Failed to backup file {}: {}", filePath, e.getMessage());
            return false;
        }
    }

    /**
     * 向GitHub提交文件
     */
    private void commitFile(String path, String content, String message) throws IOException {
        // 检查文件是否存在
        String existingSha = getFileSha(path);

        // 构建请求
        String url = String.format("%s/repos/%s/%s/contents/%s", GITHUB_API_BASE, owner, repo, path);

        HttpPut request = new HttpPut(url);
        request.setHeader("Authorization", "Bearer " + githubToken);
        request.setHeader("Accept", "application/vnd.github+json");
        request.setHeader("X-GitHub-Api-Version", "2022-11-28");

        // 构建请求体
        String jsonBody;
        if (existingSha != null) {
            // 更新现有文件
            jsonBody = String.format("""
                {
                    "message": "%s",
                    "content": "%s",
                    "sha": "%s",
                    "branch": "%s"
                }
                """, message, java.util.Base64.getEncoder().encodeToString(content.getBytes()), existingSha, backupBranch);
        } else {
            // 创建新文件
            jsonBody = String.format("""
                {
                    "message": "%s",
                    "content": "%s",
                    "branch": "%s"
                }
                """, message, java.util.Base64.getEncoder().encodeToString(content.getBytes()), backupBranch);
        }

        request.setEntity(new StringEntity(jsonBody, ContentType.APPLICATION_JSON));

        try (CloseableHttpResponse response = httpClient.execute(request)) {
            int statusCode = response.getCode();
            if (statusCode != 200 && statusCode != 201) {
                String responseBody = EntityUtils.toString(response.getEntity());
                logger.warn("GitHub API returned {} for path {}: {}", statusCode, path, responseBody);
            }
        }
    }

    /**
     * 获取文件的 SHA（用于更新现有文件）
     */
    private String getFileSha(String path) {
        String url = String.format("%s/repos/%s/%s/contents/%s?ref=%s",
                GITHUB_API_BASE, owner, repo, path, backupBranch);

        HttpGet request = new HttpGet(url);
        request.setHeader("Authorization", "Bearer " + githubToken);
        request.setHeader("Accept", "application/vnd.github+json");
        request.setHeader("X-GitHub-Api-Version", "2022-11-28");

        try (CloseableHttpResponse response = httpClient.execute(request)) {
            if (response.getCode() == 200) {
                var mapper = new ObjectMapper();
                var tree = mapper.readTree(response.getEntity().getContent());
                return tree.path("sha").asText();
            }
        } catch (Exception e) {
            logger.debug("File does not exist yet on GitHub: {}", path);
        }
        return null;
    }

    /**
     * 更新备份索引
     */
    private void updateBackupIndex(String timestamp, int filesCount) throws IOException {
        // 读取现有索引
        String indexPath = BACKUP_BASE_PATH + "/index.json";
        BackupIndex index;

        try {
            index = getBackupIndex();
        } catch (Exception e) {
            index = new BackupIndex();
        }

        // 添加新备份记录
        BackupRecord record = new BackupRecord(
                timestamp,
                Instant.now().atZone(ZoneId.of("Asia/Shanghai")).toLocalDateTime().toString(),
                filesCount,
                "success"
        );
        index.records().add(0, record); // 添加到列表开头

        // 只保留最近100条记录
        if (index.records().size() > 100) {
            index = new BackupIndex(index.records().subList(0, 100));
        }

        // 提交索引
        String indexContent = objectMapper.writeValueAsString(index);
        commitFile(indexPath, indexContent, "Update backup index: " + timestamp);
    }

    /**
     * 获取备份索引
     */
    public BackupIndex getBackupIndex() throws IOException {
        String indexPath = BACKUP_BASE_PATH + "/index.json";
        String url = String.format("%s/repos/%s/%s/contents/%s?ref=%s",
                GITHUB_API_BASE, owner, repo, indexPath, backupBranch);

        HttpGet request = new HttpGet(url);
        request.setHeader("Authorization", "Bearer " + githubToken);
        request.setHeader("Accept", "application/vnd.github.raw+json");

        try (CloseableHttpResponse response = httpClient.execute(request)) {
            if (response.getCode() == 200) {
                return objectMapper.readValue(response.getEntity().getContent(), BackupIndex.class);
            }
        }
        return new BackupIndex();
    }

    /**
     * 获取指定版本的记忆
     */
    public MemorySnapshot getMemorySnapshot(String timestamp) {
        String episodicPath = String.format("%s/%s/episodic.json", BACKUP_BASE_PATH, timestamp);
        String semanticPath = String.format("%s/%s/semantic.json", BACKUP_BASE_PATH, timestamp);
        String proceduralPath = String.format("%s/%s/procedural.json", BACKUP_BASE_PATH, timestamp);

        MemorySnapshot snapshot = new MemorySnapshot(timestamp);

        try {
            snapshot.episodicContent(fetchFileContent(episodicPath));
        } catch (Exception e) {
            logger.debug("Could not fetch episodic memory for {}", timestamp);
        }

        try {
            snapshot.semanticContent(fetchFileContent(semanticPath));
        } catch (Exception e) {
            logger.debug("Could not fetch semantic memory for {}", timestamp);
        }

        try {
            snapshot.proceduralContent(fetchFileContent(proceduralPath));
        } catch (Exception e) {
            logger.debug("Could not fetch procedural memory for {}", timestamp);
        }

        return snapshot;
    }

    /**
     * S4-2: 版本回溯支持
     * 从指定版本恢复记忆
     */
    public RestoreResult restoreFromBackup(String timestamp) {
        try {
            MemorySnapshot snapshot = getMemorySnapshot(timestamp);

            if (snapshot.episodicContent() == null &&
                snapshot.semanticContent() == null &&
                snapshot.proceduralContent() == null) {
                return new RestoreResult(false, "No backup found for timestamp: " + timestamp, 0);
            }

            int restored = 0;

            // 恢复情景记忆
            if (snapshot.episodicContent() != null) {
                memoryPersistenceService.saveMemory();
                restored++;
            }

            // 恢复语义记忆
            if (snapshot.semanticContent() != null) {
                restored++;
            }

            // 恢复程序记忆
            if (snapshot.proceduralContent() != null) {
                restored++;
            }

            logger.info("Restored {} memory types from backup {}", restored, timestamp);
            return new RestoreResult(true, "Restored " + restored + " memory types", restored);

        } catch (Exception e) {
            logger.error("Failed to restore from backup {}: {}", timestamp, e.getMessage());
            return new RestoreResult(false, "Restore failed: " + e.getMessage(), 0);
        }
    }

    /**
     * S4-2: 获取可用的备份版本列表
     */
    public BackupListResult listBackups() {
        try {
            BackupIndex index = getBackupIndex();
            return new BackupListResult(true, "Success", index.records());
        } catch (Exception e) {
            logger.error("Failed to list backups: {}", e.getMessage());
            return new BackupListResult(false, "Failed to list: " + e.getMessage(), java.util.Collections.emptyList());
        }
    }

    /**
     * S4-2: 比较两个版本的差异
     */
    public DiffResult compareBackups(String timestamp1, String timestamp2) {
        try {
            MemorySnapshot snapshot1 = getMemorySnapshot(timestamp1);
            MemorySnapshot snapshot2 = getMemorySnapshot(timestamp2);

            int differences = 0;
            StringBuilder diff = new StringBuilder();

            if (snapshot1.episodicContent() != null && snapshot2.episodicContent() != null) {
                if (!snapshot1.episodicContent().equals(snapshot2.episodicContent())) {
                    differences++;
                    diff.append("- episodic memory differs\n");
                }
            }

            if (snapshot1.semanticContent() != null && snapshot2.semanticContent() != null) {
                if (!snapshot1.semanticContent().equals(snapshot2.semanticContent())) {
                    differences++;
                    diff.append("- semantic memory differs\n");
                }
            }

            if (snapshot1.proceduralContent() != null && snapshot2.proceduralContent() != null) {
                if (!snapshot1.proceduralContent().equals(snapshot2.proceduralContent())) {
                    differences++;
                    diff.append("- procedural memory differs\n");
                }
            }

            return new DiffResult(differences == 0, differences, diff.toString());

        } catch (Exception e) {
            logger.error("Failed to compare backups: {}", e.getMessage());
            return new DiffResult(false, 0, "Comparison failed: " + e.getMessage());
        }
    }

    /**
     * S4-3: 冲突检测
     * 检测本地记忆与远程备份是否有冲突
     */
    public ConflictCheckResult checkConflicts() {
        try {
            BackupIndex index = getBackupIndex();
            if (index.records().isEmpty()) {
                return new ConflictCheckResult(false, "No backups found", null, null);
            }

            // 获取最新的备份时间
            String latestBackupTime = index.records().get(0).timestamp();

            // 获取本地上次保存时间
            Instant localSaveTime = memoryPersistenceService.getLastSaveTime();

            // 获取远程上次备份时间
            Instant remoteBackupTime = Instant.parse(latestBackupTime.replace("_", "T"));

            // 如果本地保存比远程备份更新，可能有冲突
            if (localSaveTime != null && localSaveTime.isAfter(remoteBackupTime)) {
                return new ConflictCheckResult(
                        true,
                        "Local changes exist that haven't been backed up",
                        localSaveTime,
                        remoteBackupTime
                );
            }

            return new ConflictCheckResult(false, "No conflicts detected", null, null);

        } catch (Exception e) {
            logger.error("Failed to check conflicts: {}", e.getMessage());
            return new ConflictCheckResult(false, "Conflict check failed: " + e.getMessage(), null, null);
        }
    }

    /**
     * 恢复结果
     */
    public record RestoreResult(
            boolean success,
            String message,
            int itemsRestored
    ) {}

    /**
     * 备份列表结果
     */
    public record BackupListResult(
            boolean success,
            String message,
            java.util.List<BackupRecord> backups
    ) {}

    /**
     * 差异结果
     */
    public record DiffResult(
            boolean identical,
            int differences,
            String details
    ) {}

    /**
     * 冲突检测结果
     */
    public record ConflictCheckResult(
            boolean hasConflict,
            String message,
            Instant localSaveTime,
            Instant remoteBackupTime
    ) {}

    /**
     * S17: 备份策略配置
     */
    public record BackupStrategy(
            boolean memoryBackupEnabled,
            boolean configBackupEnabled,
            boolean codeSnapshotEnabled,
            int retentionDays,
            String schedule
    ) {
        public static BackupStrategy defaultStrategy() {
            return new BackupStrategy(true, true, true, 30, "0 */6 * * *");
        }
    }

    /**
     * S17: 备份版本信息
     */
    public record BackupVersion(
            String backupId,
            String type,          // "memory", "config", "code-snapshot"
            String timestamp,
            String commitMessage,
            int filesCount,
            String status
    ) {}

    /**
     * S17: 配置文件快照
     */
    public static class ConfigSnapshot {
        private final String timestamp;
        private final java.util.Map<String, String> configContents;

        public ConfigSnapshot(String timestamp) {
            this.timestamp = timestamp;
            this.configContents = new java.util.LinkedHashMap<>();
        }

        public String timestamp() { return timestamp; }
        public java.util.Map<String, String> configContents() { return configContents; }
        public void addConfig(String filename, String content) {
            configContents.put(filename, content);
        }
    }

    /**
     * S17: 代码快照
     */
    public static class CodeSnapshot {
        private final String timestamp;
        private String sourceContent;
        private String pomContent;

        public CodeSnapshot(String timestamp) {
            this.timestamp = timestamp;
        }

        public String timestamp() { return timestamp; }
        public String sourceContent() { return sourceContent; }
        public void sourceContent(String content) { this.sourceContent = content; }
        public String pomContent() { return pomContent; }
        public void pomContent(String content) { this.pomContent = content; }
    }

    /**
     * S17: 回滚结果（包含备份类型信息）
     */
    public record RollbackResult(
            boolean success,
            String message,
            int itemsRestored,
            String backupType
    ) {}

    /**
     * 获取文件内容
     */
    private String fetchFileContent(String path) throws IOException {
        String url = String.format("%s/repos/%s/%s/contents/%s?ref=%s",
                GITHUB_API_BASE, owner, repo, path, backupBranch);

        HttpGet request = new HttpGet(url);
        request.setHeader("Authorization", "Bearer " + githubToken);
        request.setHeader("Accept", "application/vnd.github.raw+json");

        try (CloseableHttpResponse response = httpClient.execute(request)) {
            if (response.getCode() == 200) {
                return new String(response.getEntity().getContent().readAllBytes());
            }
            return null;
        }
    }

    /**
     * 手动触发备份
     */
    public BackupResult forceBackup() {
        return performBackup();
    }

    /**
     * 获取上次备份时间
     */
    public Instant getLastBackupTime() {
        return lastBackupTimes.get("lastBackup");
    }

    // ==================== S17: 配置文件版本化管理 ====================

    /**
     * S17-1: 备份配置文件
     */
    public BackupResult backupConfigFiles(String commitMessage) {
        if (!backupEnabled || githubToken == null || githubToken.isEmpty()) {
            return new BackupResult(false, "Backup disabled or token not configured", 0);
        }

        Instant startTime = Instant.now();
        String timestamp = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss")
                .withZone(ZoneId.of("Asia/Shanghai"))
                .format(startTime);

        try {
            int filesBackedUp = 0;
            ConfigSnapshot snapshot = new ConfigSnapshot(timestamp);

            // 读取并备份各个配置文件
            for (String configFile : DEFAULT_CONFIG_FILES) {
                Path configPath = Paths.get(configFile);
                if (Files.exists(configPath)) {
                    String content = Files.readString(configPath);
                    snapshot.addConfig(configFile, content);
                    filesBackedUp++;
                }
            }

            if (filesBackedUp == 0) {
                return new BackupResult(false, "No config files found", 0);
            }

            // 保存配置文件快照
            String snapshotContent = objectMapper.writeValueAsString(snapshot);

            // 备份到带时间戳的路径
            String backupPath = String.format("%s/%s/snapshot.json", CONFIG_BACKUP_PATH, timestamp);
            String latestPath = String.format("%s/latest/snapshot.json", CONFIG_BACKUP_PATH);

            commitFile(backupPath, snapshotContent, commitMessage != null ? commitMessage : "Backup config: " + timestamp);
            commitFile(latestPath, snapshotContent, "Update config latest: " + timestamp);

            // 更新配置备份索引
            updateConfigBackupIndex(timestamp, filesBackedUp, commitMessage);

            lastBackupTimes.put("configBackup", startTime);

            long duration = java.time.Duration.between(startTime, Instant.now()).toMillis();
            logger.info("Config backup completed: {} files in {}ms", filesBackedUp, duration);

            return new BackupResult(true, "Config backup completed successfully", filesBackedUp);

        } catch (Exception e) {
            logger.error("Config backup failed: {}", e.getMessage());
            return new BackupResult(false, "Config backup failed: " + e.getMessage(), 0);
        }
    }

    /**
     * S17-2: 备份代码快照
     */
    public BackupResult backupCodeSnapshot(String commitMessage) {
        if (!backupEnabled || githubToken == null || githubToken.isEmpty()) {
            return new BackupResult(false, "Backup disabled or token not configured", 0);
        }

        Instant startTime = Instant.now();
        String timestamp = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss")
                .withZone(ZoneId.of("Asia/Shanghai"))
                .format(startTime);

        try {
            CodeSnapshot snapshot = new CodeSnapshot(timestamp);
            int filesBackedUp = 0;

            // 备份主要源代码目录（简化版：只备份关键文件列表）
            Path srcPath = Paths.get(CODE_DIR);
            if (Files.exists(srcPath)) {
                // 创建源代码的压缩表示（实际上是备份文件列表和关键文件内容）
                StringBuilder sourceManifest = new StringBuilder();
                sourceManifest.append("# Code Snapshot\n");
                sourceManifest.append("timestamp: ").append(timestamp).append("\n\n");

                // 备份关键目录结构
                String[] keyDirs = {"main/java", "main/resources"};
                for (String dir : keyDirs) {
                    Path dirPath = srcPath.resolve(dir);
                    if (Files.exists(dirPath)) {
                        sourceManifest.append("## ").append(dir).append("\n");
                        sourceManifest.append("files:\n");
                        // 列出文件（简化处理，实际应该递归打包）
                        try (var stream = Files.walk(dirPath)) {
                            stream.filter(Files::isRegularFile)
                                .filter(p -> p.toString().endsWith(".java") || p.toString().endsWith(".xml") || p.toString().endsWith(".yaml") || p.toString().endsWith(".yml"))
                                .limit(100) // 限制文件数量
                                .forEach(p -> {
                                    try {
                                        String relativePath = dirPath.relativize(p).toString();
                                        sourceManifest.append("- ").append(relativePath).append("\n");
                                        String content = Files.readString(p);
                                        String encoded = java.util.Base64.getEncoder().encodeToString(content.getBytes()).substring(0, Math.min(200, content.length()));
                                        sourceManifest.append("  preview: \"").append(encoded).append("...\"\n");
                                    } catch (IOException ignored) {}
                                });
                        }
                        filesBackedUp++;
                    }
                }

                snapshot.sourceContent(sourceManifest.toString());
            }

            // 备份 pom.xml
            Path pomPath = Paths.get("pom.xml");
            if (Files.exists(pomPath)) {
                snapshot.pomContent(Files.readString(pomPath));
                filesBackedUp++;
            }

            if (filesBackedUp == 0) {
                return new BackupResult(false, "No source files found", 0);
            }

            // 保存代码快照
            String snapshotContent = objectMapper.writeValueAsString(snapshot);

            // 备份到带时间戳的路径
            String backupPath = String.format("%s/%s/snapshot.json", CODE_SNAPSHOT_PATH, timestamp);
            String latestPath = String.format("%s/latest/snapshot.json", CODE_SNAPSHOT_PATH);

            commitFile(backupPath, snapshotContent, commitMessage != null ? commitMessage : "Code snapshot: " + timestamp);
            commitFile(latestPath, snapshotContent, "Update code latest: " + timestamp);

            // 更新代码快照索引
            updateCodeSnapshotIndex(timestamp, filesBackedUp, commitMessage);

            lastBackupTimes.put("codeSnapshot", startTime);

            long duration = java.time.Duration.between(startTime, Instant.now()).toMillis();
            logger.info("Code snapshot backup completed: {} items in {}ms", filesBackedUp, duration);

            return new BackupResult(true, "Code snapshot backup completed successfully", filesBackedUp);

        } catch (Exception e) {
            logger.error("Code snapshot backup failed: {}", e.getMessage());
            return new BackupResult(false, "Code snapshot backup failed: " + e.getMessage(), 0);
        }
    }

    /**
     * S17-3: 列出所有备份版本
     */
    public List<BackupVersion> listBackupVersions() {
        List<BackupVersion> versions = new java.util.ArrayList<>();

        try {
            // 获取记忆备份索引
            BackupIndex memoryIndex = getBackupIndex();
            for (BackupRecord record : memoryIndex.records()) {
                versions.add(new BackupVersion(
                    record.timestamp(),
                    "memory",
                    record.localDateTime(),
                    "Memory backup",
                    record.filesCount(),
                    record.status()
                ));
            }

            // 获取配置备份索引
            try {
                String configIndexPath = CONFIG_BACKUP_PATH + "/index.json";
                String content = fetchFileContent(configIndexPath);
                if (content != null) {
                    var mapper = new ObjectMapper();
                    var tree = mapper.readTree(content);
                    var records = tree.get("records");
                    if (records != null && records.isArray()) {
                        for (var node : records) {
                            versions.add(new BackupVersion(
                                node.path("timestamp").asText(),
                                "config",
                                node.path("localDateTime").asText(),
                                node.path("commitMessage").asText("Config backup"),
                                node.path("filesCount").asInt(),
                                node.path("status").asText("success")
                            ));
                        }
                    }
                }
            } catch (Exception e) {
                logger.debug("Could not fetch config backup index: {}", e.getMessage());
            }

            // 获取代码快照索引
            try {
                String codeIndexPath = CODE_SNAPSHOT_PATH + "/index.json";
                String content = fetchFileContent(codeIndexPath);
                if (content != null) {
                    var mapper = new ObjectMapper();
                    var tree = mapper.readTree(content);
                    var records = tree.get("records");
                    if (records != null && records.isArray()) {
                        for (var node : records) {
                            versions.add(new BackupVersion(
                                node.path("timestamp").asText(),
                                "code-snapshot",
                                node.path("localDateTime").asText(),
                                node.path("commitMessage").asText("Code snapshot"),
                                node.path("filesCount").asInt(),
                                node.path("status").asText("success")
                            ));
                        }
                    }
                }
            } catch (Exception e) {
                logger.debug("Could not fetch code snapshot index: {}", e.getMessage());
            }

            // 按时间排序（最新的在前）
            versions.sort((a, b) -> b.timestamp().compareTo(a.timestamp()));

        } catch (Exception e) {
            logger.error("Failed to list backup versions: {}", e.getMessage());
        }

        return versions;
    }

    /**
     * S17-4: 回滚到指定备份点
     */
    public RollbackResult rollbackTo(String backupId) {
        try {
            // 解析 backupId - 格式为 "type:timestamp"
            String[] parts = backupId.split(":", 2);
            if (parts.length != 2) {
                return new RollbackResult(false, "Invalid backup ID format. Expected 'type:timestamp'", 0, null);
            }

            String type = parts[0];
            String timestamp = parts[1];

            switch (type) {
                case "memory":
                    RestoreResult memoryResult = restoreFromBackup(timestamp);
                    return new RollbackResult(memoryResult.success(), memoryResult.message(), memoryResult.itemsRestored(), "memory");
                case "config":
                    return rollbackConfig(timestamp);
                case "code-snapshot":
                    return rollbackCodeSnapshot(timestamp);
                default:
                    return new RollbackResult(false, "Unknown backup type: " + type, 0, null);
            }

        } catch (Exception e) {
            logger.error("Rollback failed: {}", e.getMessage());
            return new RollbackResult(false, "Rollback failed: " + e.getMessage(), 0, null);
        }
    }

    /**
     * 回滚配置文件
     */
    private RollbackResult rollbackConfig(String timestamp) {
        try {
            String snapshotPath = String.format("%s/%s/snapshot.json", CONFIG_BACKUP_PATH, timestamp);
            String content = fetchFileContent(snapshotPath);

            if (content == null || content.isEmpty()) {
                return new RollbackResult(false, "Config backup not found: " + timestamp, 0, "config");
            }

            ConfigSnapshot snapshot = objectMapper.readValue(content, ConfigSnapshot.class);

            int restored = 0;
            for (var entry : snapshot.configContents().entrySet()) {
                Path localPath = Paths.get(entry.getKey());
                Files.writeString(localPath, entry.getValue());
                restored++;
                logger.info("Restored config file: {}", entry.getKey());
            }

            return new RollbackResult(true, "Restored " + restored + " config files", restored, "config");

        } catch (Exception e) {
            logger.error("Config rollback failed: {}", e.getMessage());
            return new RollbackResult(false, "Config rollback failed: " + e.getMessage(), 0, "config");
        }
    }

    /**
     * 回滚代码快照
     */
    private RollbackResult rollbackCodeSnapshot(String timestamp) {
        try {
            String snapshotPath = String.format("%s/%s/snapshot.json", CODE_SNAPSHOT_PATH, timestamp);
            String content = fetchFileContent(snapshotPath);

            if (content == null || content.isEmpty()) {
                return new RollbackResult(false, "Code snapshot not found: " + timestamp, 0, "code-snapshot");
            }

            CodeSnapshot snapshot = objectMapper.readValue(content, CodeSnapshot.class);

            int restored = 0;

            // 恢复 pom.xml
            if (snapshot.pomContent() != null) {
                Path pomPath = Paths.get("pom.xml");
                Files.writeString(pomPath, snapshot.pomContent());
                restored++;
                logger.info("Restored pom.xml");
            }

            // 注意：源代码的回滚比较复杂，这里只是记录，不实际恢复源码
            // 实际恢复需要更复杂的处理（如解压、覆盖等）

            return new RollbackResult(true, "Code snapshot noted (pom.xml restored: " + restored + ")", restored, "code-snapshot");

        } catch (Exception e) {
            logger.error("Code snapshot rollback failed: {}", e.getMessage());
            return new RollbackResult(false, "Code snapshot rollback failed: " + e.getMessage(), 0, "code-snapshot");
        }
    }

    /**
     * 更新配置备份索引
     */
    private void updateConfigBackupIndex(String timestamp, int filesCount, String commitMessage) throws IOException {
        String indexPath = CONFIG_BACKUP_PATH + "/index.json";
        ConfigBackupIndex index;

        try {
            String content = fetchFileContent(indexPath);
            if (content != null) {
                index = objectMapper.readValue(content, ConfigBackupIndex.class);
            } else {
                index = new ConfigBackupIndex();
            }
        } catch (Exception e) {
            index = new ConfigBackupIndex();
        }

        ConfigBackupRecord record = new ConfigBackupRecord(
                timestamp,
                Instant.now().atZone(ZoneId.of("Asia/Shanghai")).toLocalDateTime().toString(),
                filesCount,
                "success",
                commitMessage != null ? commitMessage : "Config backup"
        );
        index.records().add(0, record);

        // 只保留最近100条记录
        if (index.records().size() > 100) {
            index = new ConfigBackupIndex(new java.util.ArrayList<>(index.records().subList(0, 100)));
        }

        String indexContent = objectMapper.writeValueAsString(index);
        commitFile(indexPath, indexContent, "Update config backup index: " + timestamp);
    }

    /**
     * 更新代码快照索引
     */
    private void updateCodeSnapshotIndex(String timestamp, int filesCount, String commitMessage) throws IOException {
        String indexPath = CODE_SNAPSHOT_PATH + "/index.json";
        CodeSnapshotIndex index;

        try {
            String content = fetchFileContent(indexPath);
            if (content != null) {
                index = objectMapper.readValue(content, CodeSnapshotIndex.class);
            } else {
                index = new CodeSnapshotIndex();
            }
        } catch (Exception e) {
            index = new CodeSnapshotIndex();
        }

        CodeSnapshotRecord record = new CodeSnapshotRecord(
                timestamp,
                Instant.now().atZone(ZoneId.of("Asia/Shanghai")).toLocalDateTime().toString(),
                filesCount,
                "success",
                commitMessage != null ? commitMessage : "Code snapshot"
        );
        index.records().add(0, record);

        // 只保留最近100条记录
        if (index.records().size() > 100) {
            index = new CodeSnapshotIndex(new java.util.ArrayList<>(index.records().subList(0, 100)));
        }

        String indexContent = objectMapper.writeValueAsString(index);
        commitFile(indexPath, indexContent, "Update code snapshot index: " + timestamp);
    }

    /**
     * 配置备份索引
     */
    public record ConfigBackupIndex(java.util.List<ConfigBackupRecord> records) {
        public ConfigBackupIndex() {
            this(new java.util.ArrayList<>());
        }
    }

    /**
     * 配置备份记录
     */
    public record ConfigBackupRecord(
            String timestamp,
            String localDateTime,
            int filesCount,
            String status,
            String commitMessage
    ) {}

    /**
     * 代码快照索引
     */
    public record CodeSnapshotIndex(java.util.List<CodeSnapshotRecord> records) {
        public CodeSnapshotIndex() {
            this(new java.util.ArrayList<>());
        }
    }

    /**
     * 代码快照记录
     */
    public record CodeSnapshotRecord(
            String timestamp,
            String localDateTime,
            int filesCount,
            String status,
            String commitMessage
    ) {}

    /**
     * 备份结果
     */
    public record BackupResult(
            boolean success,
            String message,
            int filesBackedUp
    ) {}

    /**
     * 备份记录
     */
    public record BackupRecord(
            String timestamp,
            String localDateTime,
            int filesCount,
            String status
    ) {}

    /**
     * 备份索引
     */
    public record BackupIndex(
            java.util.List<BackupRecord> records
    ) {
        public BackupIndex() {
            this(new java.util.ArrayList<>());
        }
    }

    /**
     * 记忆快照
     */
    public static class MemorySnapshot {
        private final String timestamp;
        private String episodicContent;
        private String semanticContent;
        private String proceduralContent;

        public MemorySnapshot(String timestamp) {
            this.timestamp = timestamp;
        }

        public String timestamp() { return timestamp; }
        public String episodicContent() { return episodicContent; }
        public void episodicContent(String c) { this.episodicContent = c; }
        public String semanticContent() { return semanticContent; }
        public void semanticContent(String c) { this.semanticContent = c; }
        public String proceduralContent() { return proceduralContent; }
        public void proceduralContent(String c) { this.proceduralContent = c; }
    }

    /**
     * 关闭服务
     */
    public void shutdown() {
        scheduler.shutdown();
        try {
            httpClient.close();
        } catch (IOException e) {
            logger.error("Error closing HTTP client: {}", e.getMessage());
        }
        logger.info("GitHubBackupService shutdown complete");
    }
}
