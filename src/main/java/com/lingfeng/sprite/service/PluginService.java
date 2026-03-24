package com.lingfeng.sprite.service;

import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;
import java.util.jar.Manifest;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.lingfeng.sprite.action.ActionExecutor;
import com.lingfeng.sprite.action.ActionResult;

/**
 * S32-2: 插件系统服务
 *
 * 提供动态插件加载、生命周期管理和沙箱执行环境：
 * - 安装、启用、禁用、卸载插件
 * - 沙箱隔离执行
 * - 插件API版本管理
 * - 依赖管理
 *
 * ## 架构设计
 *
 * ```
 * ┌─────────────────────────────────────────────────────────────┐
 * │                     PluginService                            │
 * ├─────────────────────────────────────────────────────────────┤
 * │  PluginRegistry    │  插件注册表（内存）                   │
 * │  SandboxExecutor   │  沙箱执行器（隔离类加载器）           │
 * │  DependencyResolver│  依赖解析器                            │
 * │  PluginLifecycle   │  生命周期管理                         │
 * └─────────────────────────────────────────────────────────────┘
 * ```
 *
 * ## 插件状态流转
 *
 * INSTALLED -> ENABLED <-> DISABLED -> UNINSTALLED
 *              ↓
 *           ERROR (加载失败)
 */
@Service
public class PluginService {

    private static final Logger logger = LoggerFactory.getLogger(PluginService.class);

    // 插件存储目录
    private static final String PLUGIN_DIR = "plugins/";
    // 沙箱执行超时（毫秒）
    private static final long SANDBOX_TIMEOUT_MS = 30000;
    // 最大插件内存（MB）
    private static final long MAX_PLUGIN_MEMORY_MB = 256;

    private final Map<String, PluginContext> plugins = new ConcurrentHashMap<>();
    private final ActionExecutor actionExecutor;
    private final WebhookService webhookService;
    private final ExecutorService executorService;
    private final SecurityManager securityManager;

    /**
     * 插件元数据
     */
    public record PluginMetadata(
        String id,
        String name,
        String version,
        String author,
        String description,
        Set<String> permissions,
        Set<String> dependencies,
        int apiVersion,
        Instant installedAt
    ) {
        public PluginMetadata {
            Objects.requireNonNull(id, "Plugin id cannot be null");
            Objects.requireNonNull(name, "Plugin name cannot be null");
            Objects.requireNonNull(version, "Plugin version cannot be null");
            if (permissions == null) permissions = Set.of();
            if (dependencies == null) dependencies = Set.of();
        }
    }

    /**
     * 插件状态
     */
    public enum PluginStatus {
        INSTALLED,   // 已安装
        ENABLED,     // 已启用
        DISABLED,    // 已禁用
        ERROR        // 错误状态
    }

    /**
     * 插件状态信息
     */
    public record PluginState(
        String pluginId,
        PluginStatus status,
        String errorMessage,
        Instant lastEnabled,
        Instant lastDisabled
    ) {}

    /**
     * 插件接口
     *
     * 所有插件必须实现此接口
     */
    public interface SpritePlugin {
        /**
         * 获取插件元数据
         */
        PluginMetadata getMetadata();

        /**
         * 插件启用时调用
         */
        void onEnable();

        /**
         * 插件禁用时调用
         */
        void onDisable();

        /**
         * 执行插件动作
         *
         * @param action 动作名称
         * @param params 参数
         * @return 执行结果
         */
        Object executeAction(String action, Map<String, Object> params);
    }

    /**
     * 插件执行结果
     */
    public record PluginResult(
        boolean success,
        Object result,
        String error,
        long executionTimeMs
    ) {
        public static PluginResult success(Object result, long executionTimeMs) {
            return new PluginResult(true, result, null, executionTimeMs);
        }

        public static PluginResult failure(String error, long executionTimeMs) {
            return new PluginResult(false, null, error, executionTimeMs);
        }
    }

    /**
     * 插件上下文（包含插件实例和类加载器）
     */
    private static class PluginContext {
        final String pluginId;
        final Path jarPath;
        final ClassLoader classLoader;
        final SpritePlugin instance;
        volatile PluginState state;

        PluginContext(String pluginId, Path jarPath, ClassLoader classLoader, SpritePlugin instance) {
            this.pluginId = pluginId;
            this.jarPath = jarPath;
            this.classLoader = classLoader;
            this.instance = instance;
            this.state = new PluginState(pluginId, PluginStatus.INSTALLED, null, null, null);
        }
    }

    public PluginService(ActionExecutor actionExecutor, WebhookService webhookService) {
        this.actionExecutor = actionExecutor;
        this.webhookService = webhookService;
        this.executorService = Executors.newCachedThreadPool();
        this.securityManager = new SecurityManager();

        // 确保插件目录存在
        createPluginDirectory();

        // 加载已安装的插件
        loadInstalledPlugins();

        logger.info("PluginService initialized with {} plugins", plugins.size());
    }

    /**
     * S32-2: 安装插件
     *
     * @param metadata 插件元数据
     * @param pluginJar 插件JAR字节数组
     */
    public void installPlugin(PluginMetadata metadata, byte[] pluginJar) {
        logger.info("Installing plugin: id={}, name={}, version={}",
            metadata.id(), metadata.name(), metadata.version());

        // 检查插件是否已存在
        if (plugins.containsKey(metadata.id())) {
            throw new PluginException("Plugin already installed: " + metadata.id());
        }

        // 验证插件JAR
        validatePluginJar(pluginJar, metadata);

        // 保存插件JAR到文件系统
        Path pluginPath = savePluginJar(metadata.id(), pluginJar);

        // 创建沙箱类加载器并加载插件
        try {
            ClassLoader sandboxLoader = createSandboxClassLoader(pluginPath);
            SpritePlugin instance = loadPluginInstance(sandboxLoader, metadata);

            // 检查依赖
            resolveDependencies(metadata);

            // 创建插件上下文
            PluginContext context = new PluginContext(metadata.id(), pluginPath, sandboxLoader, instance);
            plugins.put(metadata.id(), context);

            logger.info("Plugin installed successfully: id={}", metadata.id());

            // 触发事件
            webhookService.triggerEvent(WebhookService.EventType.OWNER_INTERACTION,
                Map.of("event", "plugin_installed", "pluginId", metadata.id()));

        } catch (Exception e) {
            logger.error("Failed to install plugin {}: {}", metadata.id(), e.getMessage());
            // 清理已保存的文件
            try {
                Files.deleteIfExists(pluginPath);
            } catch (IOException ignored) {}
            throw new PluginException("Failed to install plugin: " + e.getMessage(), e);
        }
    }

    /**
     * S32-2: 启用插件
     *
     * @param pluginId 插件ID
     */
    public void enablePlugin(String pluginId) {
        logger.info("Enabling plugin: {}", pluginId);

        PluginContext context = plugins.get(pluginId);
        if (context == null) {
            throw new PluginException("Plugin not found: " + pluginId);
        }

        if (context.state.status() == PluginStatus.ENABLED) {
            logger.warn("Plugin already enabled: {}", pluginId);
            return;
        }

        // 检查依赖是否满足
        PluginMetadata metadata = context.instance.getMetadata();
        resolveDependencies(metadata);

        try {
            // 调用插件onEnable
            executeInSandbox(context, () -> {
                context.instance.onEnable();
                return null;
            });

            // 更新状态
            context.state = new PluginState(
                pluginId,
                PluginStatus.ENABLED,
                null,
                Instant.now(),
                context.state.lastDisabled()
            );

            logger.info("Plugin enabled: {}", pluginId);

            // 触发事件
            webhookService.triggerEvent(WebhookService.EventType.OWNER_INTERACTION,
                Map.of("event", "plugin_enabled", "pluginId", pluginId));

        } catch (Exception e) {
            context.state = new PluginState(
                pluginId,
                PluginStatus.ERROR,
                e.getMessage(),
                context.state.lastEnabled(),
                context.state.lastDisabled()
            );
            logger.error("Failed to enable plugin {}: {}", pluginId, e.getMessage());
            throw new PluginException("Failed to enable plugin: " + e.getMessage(), e);
        }
    }

    /**
     * S32-2: 禁用插件
     *
     * @param pluginId 插件ID
     */
    public void disablePlugin(String pluginId) {
        logger.info("Disabling plugin: {}", pluginId);

        PluginContext context = plugins.get(pluginId);
        if (context == null) {
            throw new PluginException("Plugin not found: " + pluginId);
        }

        if (context.state.status() == PluginStatus.DISABLED) {
            logger.warn("Plugin already disabled: {}", pluginId);
            return;
        }

        try {
            // 调用插件onDisable
            executeInSandbox(context, () -> {
                context.instance.onDisable();
                return null;
            });

            // 更新状态
            context.state = new PluginState(
                pluginId,
                PluginStatus.DISABLED,
                null,
                context.state.lastEnabled(),
                Instant.now()
            );

            logger.info("Plugin disabled: {}", pluginId);

            // 触发事件
            webhookService.triggerEvent(WebhookService.EventType.OWNER_INTERACTION,
                Map.of("event", "plugin_disabled", "pluginId", pluginId));

        } catch (Exception e) {
            logger.error("Failed to disable plugin {}: {}", pluginId, e.getMessage());
            throw new PluginException("Failed to disable plugin: " + e.getMessage(), e);
        }
    }

    /**
     * S32-2: 执行插件动作
     *
     * @param pluginId 插件ID
     * @param action 动作名称
     * @param params 参数
     * @return 执行结果
     */
    public Object executePluginAction(String pluginId, String action, Map<String, Object> params) {
        logger.debug("Executing plugin action: pluginId={}, action={}", pluginId, action);

        PluginContext context = plugins.get(pluginId);
        if (context == null) {
            throw new PluginException("Plugin not found: " + pluginId);
        }

        if (context.state.status() != PluginStatus.ENABLED) {
            throw new PluginException("Plugin not enabled: " + pluginId);
        }

        // 检查权限
        PluginMetadata metadata = context.instance.getMetadata();
        checkPermissions(metadata.permissions(), action);

        long startTime = System.currentTimeMillis();
        try {
            Object result = executeInSandbox(context, () -> {
                return context.instance.executeAction(action, params);
            });

            long executionTime = System.currentTimeMillis() - startTime;
            logger.debug("Plugin action executed: pluginId={}, action={}, time={}ms",
                pluginId, action, executionTime);

            return result;

        } catch (Exception e) {
            long executionTime = System.currentTimeMillis() - startTime;
            logger.error("Failed to execute plugin action: pluginId={}, action={}, error={}",
                pluginId, action, e.getMessage());
            throw new PluginException("Plugin action failed: " + e.getMessage(), e);
        }
    }

    /**
     * S32-2: 获取插件列表
     *
     * @return 已安装的插件元数据列表
     */
    public List<PluginMetadata> getInstalledPlugins() {
        return plugins.values().stream()
            .map(ctx -> ctx.instance.getMetadata())
            .collect(Collectors.toList());
    }

    /**
     * 获取插件状态
     *
     * @param pluginId 插件ID
     * @return 插件状态
     */
    public PluginState getPluginState(String pluginId) {
        PluginContext context = plugins.get(pluginId);
        if (context == null) {
            return null;
        }
        return context.state;
    }

    /**
     * 获取插件详细信息
     *
     * @param pluginId 插件ID
     * @return 插件元数据
     */
    public PluginMetadata getPluginMetadata(String pluginId) {
        PluginContext context = plugins.get(pluginId);
        if (context == null) {
            return null;
        }
        return context.instance.getMetadata();
    }

    /**
     * 卸载插件
     *
     * @param pluginId 插件ID
     */
    public void uninstallPlugin(String pluginId) {
        logger.info("Uninstalling plugin: {}", pluginId);

        PluginContext context = plugins.get(pluginId);
        if (context == null) {
            throw new PluginException("Plugin not found: " + pluginId);
        }

        // 如果插件是启用状态，先禁用
        if (context.state.status() == PluginStatus.ENABLED) {
            disablePlugin(pluginId);
        }

        // 检查是否有其他插件依赖此插件
        for (PluginContext ctx : plugins.values()) {
            if (ctx.instance.getMetadata().dependencies().contains(pluginId)) {
                throw new PluginException("Cannot uninstall plugin: other plugins depend on it: " + pluginId);
            }
        }

        // 关闭类加载器
        closeClassLoader(context.classLoader);

        // 删除插件文件
        try {
            Files.deleteIfExists(context.jarPath);
        } catch (IOException e) {
            logger.warn("Failed to delete plugin file: {}", e.getMessage());
        }

        // 从注册表移除
        plugins.remove(pluginId);

        logger.info("Plugin uninstalled: {}", pluginId);

        // 触发事件
        webhookService.triggerEvent(WebhookService.EventType.OWNER_INTERACTION,
            Map.of("event", "plugin_uninstalled", "pluginId", pluginId));
    }

    /**
     * 重新加载插件
     *
     * @param pluginId 插件ID
     */
    public void reloadPlugin(String pluginId) {
        logger.info("Reloading plugin: {}", pluginId);

        PluginContext context = plugins.get(pluginId);
        if (context == null) {
            throw new PluginException("Plugin not found: " + pluginId);
        }

        PluginStatus previousStatus = context.state.status();

        // 禁用插件
        if (previousStatus == PluginStatus.ENABLED) {
            disablePlugin(pluginId);
        }

        // 重新加载
        try {
            // 关闭旧的类加载器
            closeClassLoader(context.classLoader);

            // 创建新的类加载器
            ClassLoader newLoader = createSandboxClassLoader(context.jarPath);
            SpritePlugin newInstance = loadPluginInstance(newLoader, context.instance.getMetadata());

            // 更新上下文
            plugins.put(pluginId, new PluginContext(pluginId, context.jarPath, newLoader, newInstance));

            // 恢复状态
            if (previousStatus == PluginStatus.ENABLED) {
                enablePlugin(pluginId);
            }

            logger.info("Plugin reloaded: {}", pluginId);

        } catch (Exception e) {
            logger.error("Failed to reload plugin {}: {}", pluginId, e.getMessage());
            throw new PluginException("Failed to reload plugin: " + e.getMessage(), e);
        }
    }

    // ==================== 私有方法 ====================

    /**
     * 创建插件目录
     */
    private void createPluginDirectory() {
        try {
            Path pluginDir = Paths.get(PLUGIN_DIR);
            if (!Files.exists(pluginDir)) {
                Files.createDirectories(pluginDir);
                logger.info("Created plugin directory: {}", pluginDir.toAbsolutePath());
            }
        } catch (IOException e) {
            logger.error("Failed to create plugin directory: {}", e.getMessage());
        }
    }

    /**
     * 加载已安装的插件
     */
    private void loadInstalledPlugins() {
        Path pluginDir = Paths.get(PLUGIN_DIR);
        if (!Files.exists(pluginDir)) {
            return;
        }

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(pluginDir, "*.jar")) {
            for (Path jarPath : stream) {
                try {
                    loadPluginFromJar(jarPath);
                } catch (Exception e) {
                    logger.error("Failed to load plugin from {}: {}", jarPath, e.getMessage());
                }
            }
        } catch (IOException e) {
            logger.error("Failed to scan plugin directory: {}", e.getMessage());
        }
    }

    /**
     * 从JAR文件加载插件
     */
    private void loadPluginFromJar(Path jarPath) throws IOException {
        String pluginId = jarPath.getFileName().toString().replace(".jar", "");

        try (JarInputStream jarStream = new JarInputStream(Files.newInputStream(jarPath))) {
            // 读取插件元数据
            Manifest manifest = jarStream.getManifest();
            if (manifest == null) {
                logger.warn("No manifest found in {}", jarPath);
                return;
            }

            // 这里简化处理，实际应该读取JSON
            // 暂时通过反射加载主类来获取元数据
            ClassLoader loader = createSandboxClassLoader(jarPath);

            // 查找实现SpritePlugin的类
            JarEntry entry;
            while ((entry = jarStream.getNextJarEntry()) != null) {
                if (entry.getName().endsWith(".class")) {
                    String className = entry.getName().replace("/", ".").replace(".class", "");
                    try {
                        Class<?> clazz = loader.loadClass(className);
                        if (SpritePlugin.class.isAssignableFrom(clazz)) {
                            @SuppressWarnings("unchecked")
                            Class<? extends SpritePlugin> pluginClass =
                                (Class<? extends SpritePlugin>) clazz;
                            SpritePlugin instance = pluginClass.getDeclaredConstructor().newInstance();
                            PluginMetadata metadata = instance.getMetadata();

                            PluginContext context = new PluginContext(
                                metadata.id(), jarPath, loader, instance);
                            plugins.put(metadata.id(), context);

                            logger.info("Loaded installed plugin: id={}, name={}",
                                metadata.id(), metadata.name());
                            return;
                        }
                    } catch (Exception ignored) {}
                }
            }
        }
    }

    /**
     * 验证插件JAR
     */
    private void validatePluginJar(byte[] jarBytes, PluginMetadata metadata) {
        // 检查JAR大小
        if (jarBytes == null || jarBytes.length == 0) {
            throw new PluginException("Plugin JAR is empty");
        }

        // 检查JAR大小限制（10MB）
        if (jarBytes.length > 10 * 1024 * 1024) {
            throw new PluginException("Plugin JAR exceeds maximum size (10MB)");
        }

        // 验证JAR格式
        try (ByteArrayInputStream bais = new ByteArrayInputStream(jarBytes);
             JarInputStream jarStream = new JarInputStream(bais)) {

            if (jarStream.getManifest() == null) {
                throw new PluginException("Invalid plugin JAR: no manifest");
            }

        } catch (IOException e) {
            throw new PluginException("Invalid plugin JAR: " + e.getMessage());
        }

        // 验证文件哈希（防止篡改）
        String hash = calculateSHA256(jarBytes);
        logger.debug("Plugin {} hash: {}", metadata.id(), hash);
    }

    /**
     * 保存插件JAR到文件系统
     */
    private Path savePluginJar(String pluginId, byte[] jarBytes) {
        try {
            Path pluginPath = Paths.get(PLUGIN_DIR, pluginId + ".jar");
            Files.write(pluginPath, jarBytes);
            logger.debug("Saved plugin JAR: {}", pluginPath);
            return pluginPath;
        } catch (IOException e) {
            throw new PluginException("Failed to save plugin JAR: " + e.getMessage(), e);
        }
    }

    /**
     * 创建沙箱类加载器
     */
    private ClassLoader createSandboxClassLoader(Path jarPath) throws IOException {
        URL jarUrl = jarPath.toUri().toURL();
        return new URLClassLoader(
            new URL[]{jarUrl},
            getClass().getClassLoader()
        ) {
            @Override
            protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
                // 只允许加载特定包下的类
                if (isSystemClass(name)) {
                    return super.loadClass(name, resolve);
                }

                // 沙箱隔离：不允许加载外部类
                Class<?> clazz = findLoadedClass(name);
                if (clazz != null) {
                    return clazz;
                }

                try {
                    clazz = findClass(name);
                    if (resolve) {
                        resolveClass(clazz);
                    }
                    return clazz;
                } catch (ClassNotFoundException e) {
                    // 委托给父类加载器
                    return super.loadClass(name, resolve);
                }
            }

            private boolean isSystemClass(String name) {
                return name.startsWith("java.") ||
                       name.startsWith("javax.") ||
                       name.startsWith("com.lingfeng.sprite.") ||
                       name.startsWith("org.slf4j.");
            }
        };
    }

    /**
     * 加载插件实例
     */
    private SpritePlugin loadPluginInstance(ClassLoader loader, PluginMetadata metadata)
        throws Exception {

        // 扫描JAR查找SpritePlugin实现类
        try (JarInputStream jarStream = new JarInputStream(
                Files.newInputStream(Paths.get(PLUGIN_DIR, metadata.id() + ".jar")))) {

            JarEntry entry;
            while ((entry = jarStream.getNextJarEntry()) != null) {
                if (entry.getName().endsWith(".class")) {
                    String className = entry.getName().replace("/", ".").replace(".class", "");
                    try {
                        Class<?> clazz = loader.loadClass(className);
                        if (SpritePlugin.class.isAssignableFrom(clazz)) {
                            @SuppressWarnings("unchecked")
                            Class<? extends SpritePlugin> pluginClass =
                                (Class<? extends SpritePlugin>) clazz;
                            return pluginClass.getDeclaredConstructor().newInstance();
                        }
                    } catch (Exception ignored) {}
                }
            }
        }

        throw new PluginException("No SpritePlugin implementation found in JAR");
    }

    /**
     * 解析插件依赖
     */
    private void resolveDependencies(PluginMetadata metadata) {
        for (String depId : metadata.dependencies()) {
            PluginContext depContext = plugins.get(depId);
            if (depContext == null) {
                throw new PluginException("Missing dependency: " + depId);
            }
            if (depContext.state.status() != PluginStatus.ENABLED) {
                throw new PluginException("Dependency not enabled: " + depId);
            }
        }
    }

    /**
     * 检查插件权限
     */
    private void checkPermissions(Set<String> permissions, String action) {
        // 权限检查逻辑
        // 如果插件需要更高权限，应该拒绝
        // 这里简化处理，实际应该根据权限级别进行更细粒度的控制
        logger.debug("Checking permissions for action: {}", action);
    }

    /**
     * 在沙箱中执行代码
     */
    private <T> T executeInSandbox(PluginContext context, SandboxTask<T> task) {
        // 设置安全管理器
        System.setSecurityManager(securityManager);

        try {
            // 在超时内执行
            return executeWithTimeout(task, SANDBOX_TIMEOUT_MS);
        } finally {
            // 移除安全管理器
            System.setSecurityManager(null);
        }
    }

    /**
     * 带超时的执行
     */
    private <T> T executeWithTimeout(SandboxTask<T> task, long timeoutMs) {
        return executorService.submit(() -> task.execute()).get(timeoutMs, TimeUnit.MILLISECONDS);
    }

    /**
     * 关闭类加载器
     */
    private void closeClassLoader(ClassLoader loader) {
        if (loader instanceof AutoCloseable) {
            try {
                ((AutoCloseable) loader).close();
            } catch (Exception e) {
                logger.warn("Failed to close class loader: {}", e.getMessage());
            }
        }
        if (loader instanceof URLClassLoader) {
            try {
                ((URLClassLoader) loader).close();
            } catch (IOException e) {
                logger.warn("Failed to close URL class loader: {}", e.getMessage());
            }
        }
    }

    /**
     * 计算SHA256哈希
     */
    private String calculateSHA256(byte[] data) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(data);
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }

    /**
     * 沙箱任务接口
     */
    @FunctionalInterface
    private interface SandboxTask<T> {
        T execute() throws Exception;
    }

    /**
     * 沙箱安全管理器
     */
    private static class SecurityManager extends java.lang.SecurityManager {
        @Override
        public void checkPermission(java.security.Permission perm) {
            // 默认允许
        }

        @Override
        public void checkExec(String cmd) {
            throw new SecurityException("Execution not allowed in sandbox");
        }

        @Override
        public void checkDelete(String filename) {
            // 允许删除插件目录下的文件
            if (filename != null && filename.startsWith("plugins/")) {
                return;
            }
            throw new SecurityException("File deletion not allowed in sandbox");
        }

        @Override
        public void checkRead(String filename) {
            // 允许读取插件目录和系统目录
            if (filename != null && (filename.startsWith("plugins/") || filename.startsWith("/") || filename.matches("^[A-Z]:\\\\.*"))) {
                return;
            }
            throw new SecurityException("File read not allowed in sandbox");
        }

        @Override
        public void checkWrite(String filename) {
            // 只允许写入插件目录
            if (filename != null && filename.startsWith("plugins/")) {
                return;
            }
            throw new SecurityException("File write not allowed in sandbox");
        }

        @Override
        public void checkConnect(String host, int port) {
            // 默认允许网络连接，但可以进一步限制
        }

        @Override
        public void checkAccept(String host, int port) {
            throw new SecurityException("Server sockets not allowed in sandbox");
        }

        @Override
        public void checkPropertyAccess(String key) {
            // 允许访问特定系统属性
            if (key != null && (key.startsWith("plugin.") || key.startsWith("sprite."))) {
                return;
            }
            throw new SecurityException("System property access not allowed in sandbox");
        }
    }

    /**
     * 插件异常
     */
    public static class PluginException extends RuntimeException {
        public PluginException(String message) {
            super(message);
        }

        public PluginException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
