package com.lingfeng.sprite.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

/**
 * HotReloadConfigService 单元测试
 */
class HotReloadConfigServiceTest {

    private HotReloadConfigService configService;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        configService = new HotReloadConfigService();
    }

    @Test
    void testLoadConfigJson() throws Exception {
        Path configFile = tempDir.resolve("test-config.json");
        String content = "{\"name\": \"test\", \"value\": 123}";
        Files.writeString(configFile, content);

        HotReloadConfigService.ConfigEntry entry = configService.loadConfig(configFile.toString());

        assertNotNull(entry);
        assertEquals(configFile.toString(), entry.path());
        assertEquals(content, entry.content());
        assertNotNull(entry.lastModified());
        assertNotNull(entry.lastLoaded());
        assertNotNull(entry.data());
        assertEquals("test", entry.data().get("name"));
        assertEquals(123, entry.data().get("value"));
    }

    @Test
    void testLoadConfigYaml() throws Exception {
        Path configFile = tempDir.resolve("test-config.yaml");
        String content = "name: test\nvalue: 456";
        Files.writeString(configFile, content);

        HotReloadConfigService.ConfigEntry entry = configService.loadConfig(configFile.toString());

        assertNotNull(entry);
        assertEquals("test", entry.data().get("name"));
        assertEquals(456, entry.data().get("value"));
    }

    @Test
    void testLoadConfigNotFound() {
        HotReloadConfigService.ConfigEntry entry = configService.loadConfig("/non/existent/path.json");
        assertNull(entry);
    }

    @Test
    void testGetConfig() throws Exception {
        Path configFile = tempDir.resolve("test-config.json");
        String content = "{\"key\": \"value\"}";
        Files.writeString(configFile, content);
        configService.loadConfig(configFile.toString());

        Map<String, Object> config = configService.getConfig(configFile.toString());
        assertNotNull(config);
        assertEquals("value", config.get("key"));
    }

    @Test
    void testGetConfigNotLoaded() {
        Map<String, Object> config = configService.getConfig("/non/existent/path.json");
        assertNull(config);
    }

    @Test
    void testGetConfigEntry() throws Exception {
        Path configFile = tempDir.resolve("test-config.json");
        String content = "{\"name\": \"test\"}";
        Files.writeString(configFile, content);

        HotReloadConfigService.ConfigEntry entry = configService.loadConfig(configFile.toString());
        HotReloadConfigService.ConfigEntry retrieved = configService.getConfigEntry(configFile.toString());

        assertNotNull(retrieved);
        assertEquals(entry.path(), retrieved.path());
        assertEquals(entry.content(), retrieved.content());
    }

    @Test
    void testSaveConfig() throws Exception {
        Path configFile = tempDir.resolve("save-test.json");
        Map<String, Object> data = Map.of("name", "saved", "count", 42);

        configService.saveConfig(configFile.toString(), data);

        Map<String, Object> loaded = configService.getConfig(configFile.toString());
        assertNotNull(loaded);
        assertEquals("saved", loaded.get("name"));
        assertEquals(42, loaded.get("count"));
    }

    @Test
    void testSaveConfigYaml() throws Exception {
        Path configFile = tempDir.resolve("save-test.yaml");
        Map<String, Object> data = Map.of("name", "yaml-saved", "count", 100);

        configService.saveConfig(configFile.toString(), data);

        Map<String, Object> loaded = configService.getConfig(configFile.toString());
        assertNotNull(loaded);
        assertEquals("yaml-saved", loaded.get("name"));
        assertEquals(100, loaded.get("count"));
    }

    @Test
    void testUpdateValue() throws Exception {
        Path configFile = tempDir.resolve("update-test.json");
        String initialContent = "{\"name\": \"original\", \"nested\": {\"key\": \"old\"}}";
        Files.writeString(configFile, initialContent);
        configService.loadConfig(configFile.toString());

        configService.updateValue(configFile.toString(), "name", "updated");
        configService.updateValue(configFile.toString(), "nested.key", "new");

        Map<String, Object> loaded = configService.getConfig(configFile.toString());
        assertEquals("updated", loaded.get("name"));
        @SuppressWarnings("unchecked")
        Map<String, Object> nested = (Map<String, Object>) loaded.get("nested");
        assertEquals("new", nested.get("key"));
    }

    @Test
    void testUpdateValueNonExistentConfig() throws Exception {
        // Should not throw, creates new config
        configService.updateValue("/non/existent/config.json", "key", "value");
        // No assertion needed - just verify no exception thrown
    }

    @Test
    void testRegisterCallback() {
        AtomicBoolean called = new AtomicBoolean(false);
        CountDownLatch latch = new CountDownLatch(1);

        configService.registerCallback("/test/path", (path, data) -> {
            called.set(true);
            latch.countDown();
        });

        // Manually trigger callback for testing
        configService.unregisterCallback("/test/path");
        // Callback was removed, so it won't be called
        assertFalse(called.get());
    }

    @Test
    void testUnregisterCallback() {
        configService.registerCallback("/test/path", (path, data) -> {});
        configService.unregisterCallback("/test/path");
        // Should not throw
    }

    @Test
    void testBackupConfig() throws Exception {
        Path configFile = tempDir.resolve("backup-test.json");
        String content = "{\"name\": \"backup-test\"}";
        Files.writeString(configFile, content);
        configService.loadConfig(configFile.toString());

        configService.backupConfig(configFile.toString());

        List<String> backups = configService.listBackups(configFile.toString());
        assertFalse(backups.isEmpty());
        assertTrue(backups.get(0).startsWith("backup-test.json.backup-"));
    }

    @Test
    void testBackupConfigNotLoaded() {
        // Should not throw
        configService.backupConfig("/non/existent/config.json");
    }

    @Test
    void testListBackups() throws Exception {
        Path configFile = tempDir.resolve("list-backup-test.json");
        Files.writeString(configFile, "{\"test\": true}");

        // Create some backup files manually
        Path backup1 = tempDir.resolve("list-backup-test.json.backup-1000");
        Path backup2 = tempDir.resolve("list-backup-test.json.backup-2000");
        Files.writeString(backup1, "backup1");
        Files.writeString(backup2, "backup2");

        List<String> backups = configService.listBackups(configFile.toString());
        assertEquals(2, backups.size());
    }

    @Test
    void testGetStats() {
        HotReloadConfigService.ConfigStats stats = configService.getStats();
        assertEquals(0, stats.loadedConfigs());
        assertNotNull(stats.lastCheck());
        assertEquals(0, stats.totalCallbacks());
    }

    @Test
    void testStopWatching() {
        configService.stopWatching();
        // Should not throw
    }

    @Test
    void testConfigCallbackInterface() {
        // Test that ConfigCallback is a functional interface
        HotReloadConfigService.ConfigCallback callback = (path, data) -> {
            assertEquals("/test/path", path);
            assertNotNull(data);
        };
        callback.onConfigChanged("/test/path", Map.of("key", "value"));
    }

    @Test
    void testConfigEntryRecord() {
        HotReloadConfigService.ConfigEntry entry = new HotReloadConfigService.ConfigEntry(
            "/path/to/config.json",
            "{\"test\": true}",
            java.time.Instant.now(),
            java.time.Instant.now(),
            Map.of("test", true)
        );

        assertEquals("/path/to/config.json", entry.path());
        assertEquals("{\"test\": true}", entry.content());
        assertNotNull(entry.lastModified());
        assertNotNull(entry.lastLoaded());
        assertEquals(Map.of("test", true), entry.data());
    }
}
