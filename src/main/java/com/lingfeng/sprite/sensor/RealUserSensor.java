package com.lingfeng.sprite.sensor;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.lingfeng.sprite.PerceptionSystem;
import com.lingfeng.sprite.PerceptionSystem.AppType;
import com.lingfeng.sprite.PerceptionSystem.Perception;
import com.lingfeng.sprite.PerceptionSystem.PresenceStatus;
import com.lingfeng.sprite.PerceptionSystem.UserPerception;
import com.lingfeng.sprite.PerceptionSystem.UserSensor;
import com.lingfeng.sprite.PerceptionSystem.WindowInfo;

import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.Structure;
import com.sun.jna.Structure.FieldOrder;
import com.sun.jna.win32.W32APIOptions;

/**
 * 真实用户传感器 - Windows/Linux 双平台实现
 *
 * 使用 JNA 调用 Windows API 获取：
 * - 当前活动窗口信息
 * - 用户空闲时间
 * - 进程信息
 *
 * Linux 环境下使用 xdotool 和 /proc 获取数据
 */
public class RealUserSensor extends UserSensor {

    private static final Logger logger = LoggerFactory.getLogger(RealUserSensor.class);

    private static final int IDLE_THRESHOLD_SECONDS = 300; // 5分钟空闲

    private static final boolean IS_WINDOWS = System.getProperty("os.name", "").toLowerCase().contains("windows");
    private static final boolean IS_LINUX = System.getProperty("os.name", "").toLowerCase().contains("linux");

    // Windows API 接口 - Kernel32
    private interface Kernel32 extends Library {
        Kernel32 INSTANCE = Native.load("kernel32", Kernel32.class, W32APIOptions.DEFAULT_OPTIONS);

        int GetLastInputInfo(LASTINPUTINFO plii);
        long GetTickCount64();
    }

    @FieldOrder({"cbSize", "dwTime"})
    public static class LASTINPUTINFO extends Structure {
        public int cbSize = 8;
        public int dwTime;
    }

    // Windows API 接口 - User32
    private interface User32 extends Library {
        User32 INSTANCE = Native.load("user32", User32.class, W32APIOptions.DEFAULT_OPTIONS);

        Pointer GetForegroundWindow();
        int GetWindowTextW(Pointer hWnd, char[] lpString, int nMaxCount);
        int GetWindowThreadProcessId(Pointer hWnd, int[] lpdwProcessId);
        Pointer GetShellWindow();
    }

    // 应用类型关键词映射
    private static final Set<String> BROWSER_KEYWORDS = new HashSet<>();
    private static final Set<String> DEV_KEYWORDS = new HashSet<>();
    private static final Set<String> CHAT_KEYWORDS = new HashSet<>();
    private static final Set<String> PRODUCTIVITY_KEYWORDS = new HashSet<>();
    private static final Set<String> MEDIA_KEYWORDS = new HashSet<>();
    private static final Set<String> SYSTEM_KEYWORDS = new HashSet<>();

    static {
        // 浏览器关键词
        BROWSER_KEYWORDS.add("chrome");
        BROWSER_KEYWORDS.add("firefox");
        BROWSER_KEYWORDS.add("edge");
        BROWSER_KEYWORDS.add("browser");
        BROWSER_KEYWORDS.add("safari");
        BROWSER_KEYWORDS.add("opera");
        BROWSER_KEYWORDS.add("brave");

        // 开发工具关键词
        DEV_KEYWORDS.add("idea");
        DEV_KEYWORDS.add("vscode");
        DEV_KEYWORDS.add("eclipse");
        DEV_KEYWORDS.add("visual studio");
        DEV_KEYWORDS.add("xcode");
        DEV_KEYWORDS.add("android studio");
        DEV_KEYWORDS.add("pycharm");
        DEV_KEYWORDS.add("webstorm");
        DEV_KEYWORDS.add("terminal");
        DEV_KEYWORDS.add("cmd");
        DEV_KEYWORDS.add("powershell");
        DEV_KEYWORDS.add("git");

        // 聊天工具关键词
        CHAT_KEYWORDS.add("wechat");
        CHAT_KEYWORDS.add("weixin");
        CHAT_KEYWORDS.add("dingtalk");
        CHAT_KEYWORDS.add("钉钉");
        CHAT_KEYWORDS.add("feishu");
        CHAT_KEYWORDS.add("飞书");
        CHAT_KEYWORDS.add("slack");
        CHAT_KEYWORDS.add("discord");
        CHAT_KEYWORDS.add("telegram");
        CHAT_KEYWORDS.add("wx");
        CHAT_KEYWORDS.add("qq");

        // 生产力工具关键词
        PRODUCTIVITY_KEYWORDS.add("word");
        PRODUCTIVITY_KEYWORDS.add("excel");
        PRODUCTIVITY_KEYWORDS.add("ppt");
        PRODUCTIVITY_KEYWORDS.add("notion");
        PRODUCTIVITY_KEYWORDS.add("obsidian");
        PRODUCTIVITY_KEYWORDS.add("evernote");
        PRODUCTIVITY_KEYWORDS.add("印象笔记");
        PRODUCTIVITY_KEYWORDS.add("有道云");
        PRODUCTIVITY_KEYWORDS.add("onenote");
        PRODUCTIVITY_KEYWORDS.add("wps");
        PRODUCTIVITY_KEYWORDS.add("pdf");
        PRODUCTIVITY_KEYWORDS.add("adobe");

        // 媒体关键词
        MEDIA_KEYWORDS.add("music");
        MEDIA_KEYWORDS.add("player");
        MEDIA_KEYWORDS.add("网易云");
        MEDIA_KEYWORDS.add("qq音乐");
        MEDIA_KEYWORDS.add("spotify");
        MEDIA_KEYWORDS.add("video");
        MEDIA_KEYWORDS.add("vlc");
        MEDIA_KEYWORDS.add("potplayer");
        MEDIA_KEYWORDS.add("视频");
        MEDIA_KEYWORDS.add("腾讯视频");
        MEDIA_KEYWORDS.add("爱奇艺");
        MEDIA_KEYWORDS.add("优酷");
        MEDIA_KEYWORDS.add("哔哩");
        MEDIA_KEYWORDS.add("bilibili");
        MEDIA_KEYWORDS.add("youtube");

        // 系统工具关键词
        SYSTEM_KEYWORDS.add("settings");
        SYSTEM_KEYWORDS.add("控制面板");
        SYSTEM_KEYWORDS.add("explorer");
        SYSTEM_KEYWORDS.add("此电脑");
        SYSTEM_KEYWORDS.add("资源管理器");
        SYSTEM_KEYWORDS.add("taskmgr");
        SYSTEM_KEYWORDS.add("任务管理器");
        SYSTEM_KEYWORDS.add("services");
        SYSTEM_KEYWORDS.add("computer");
    }

    public RealUserSensor() {
        super();
    }

    @Override
    public Perception perceive() {
        try {
            // 获取活动窗口信息
            WindowInfo windowInfo = getActiveWindowInfo();

            // 获取用户空闲状态
            PresenceStatus presence = getPresenceStatus();

            return new Perception(
                    Instant.now(),
                    null,
                    new UserPerception(
                            windowInfo,
                            presence,
                            java.util.List.of(), // recentCommands - 可扩展
                            null, // voiceInput
                            java.util.List.of()  // gestures
                    ),
                    null, null, null, null
            );
        } catch (Exception e) {
            logger.warn("Failed to get user perception: {}", e.getMessage());
            return getDefaultPerception();
        }
    }

    /**
     * 获取活动窗口信息
     */
    private WindowInfo getActiveWindowInfo() {
        if (IS_WINDOWS) {
            return getActiveWindowInfoWindows();
        } else if (IS_LINUX) {
            return getActiveWindowInfoLinux();
        } else {
            logger.debug("Unsupported platform detected, returning default window info");
            return new WindowInfo("Unknown", "Unknown", AppType.UNKNOWN, false);
        }
    }

    /**
     * Windows: 获取活动窗口信息
     */
    private WindowInfo getActiveWindowInfoWindows() {
        try {
            User32 user32 = User32.INSTANCE;

            // 获取前台窗口句柄
            Pointer hWnd = user32.GetForegroundWindow();
            if (hWnd == null) {
                return new WindowInfo("Unknown", "Unknown", AppType.UNKNOWN, false);
            }

            // 获取窗口标题
            char[] titleBuffer = new char[512];
            int titleLength = user32.GetWindowTextW(hWnd, titleBuffer, titleBuffer.length);
            String title = titleLength > 0 ? new String(titleBuffer, 0, titleLength) : "Unknown";

            // 获取进程ID和进程名
            int[] processId = new int[1];
            user32.GetWindowThreadProcessId(hWnd, processId);
            String processName = getProcessName(processId[0]);

            // 判断是否聚焦
            boolean isFocused = hWnd != null;

            // 分类应用类型
            AppType appType = classifyApp(processName.toLowerCase(), title.toLowerCase());

            return new WindowInfo(title, processName, appType, isFocused);

        } catch (Exception e) {
            logger.warn("Failed to get active window: {}", e.getMessage());
            return new WindowInfo("Unknown", "Unknown", AppType.UNKNOWN, false);
        }
    }

    /**
     * S5-1: Linux: 获取活动窗口信息
     */
    private WindowInfo getActiveWindowInfoLinux() {
        try {
            // 使用 xdotool 获取活动窗口信息
            // 获取窗口标题
            String title = executeCommand("xdotool", "getactivewindow", "getwindowname");
            if (title == null || title.isEmpty()) {
                title = "Unknown";
            }

            // 获取窗口 PID
            String pidStr = executeCommand("xdotool", "getactivewindow", "getwindowpid");
            String processName = "Unknown";
            int pid = 0;

            if (pidStr != null && !pidStr.isEmpty()) {
                try {
                    pid = Integer.parseInt(pidStr.trim());
                    processName = getProcessNameLinux(pid);
                } catch (NumberFormatException e) {
                    logger.debug("Failed to parse PID: {}", pidStr);
                }
            }

            // 分类应用类型
            AppType appType = classifyApp(processName.toLowerCase(), title.toLowerCase());

            return new WindowInfo(title.trim(), processName, appType, true);

        } catch (Exception e) {
            logger.warn("Failed to get active window on Linux: {}", e.getMessage());
            return new WindowInfo("Unknown", "Unknown", AppType.UNKNOWN, false);
        }
    }

    /**
     * S5-1: Linux: 根据 PID 获取进程名
     */
    private String getProcessNameLinux(int pid) {
        try {
            // 读取 /proc/PID/comm 获取进程名
            String commPath = "/proc/" + pid + "/comm";
            String processName = readFile(commPath);
            if (processName != null && !processName.isEmpty()) {
                return processName.trim();
            }

            // 备用：读取 cmdline
            String cmdlinePath = "/proc/" + pid + "/cmdline";
            String cmdline = readFile(cmdlinePath);
            if (cmdline != null && !cmdline.isEmpty()) {
                // cmdline 以 null 字符分隔，取第一个（程序路径）
                String[] parts = cmdline.split("\0");
                if (parts.length > 0) {
                    String fullPath = parts[0];
                    // 提取文件名
                    int lastSlash = fullPath.lastIndexOf('/');
                    if (lastSlash >= 0 && lastSlash < fullPath.length() - 1) {
                        return fullPath.substring(lastSlash + 1);
                    }
                    return fullPath;
                }
            }
        } catch (Exception e) {
            logger.debug("Failed to get process name for PID {}: {}", pid, e.getMessage());
        }
        return "Unknown";
    }

    /**
     * 读取文件内容
     */
    private String readFile(String path) {
        try {
            java.nio.file.Path p = java.nio.file.Paths.get(path);
            if (java.nio.file.Files.exists(p)) {
                return java.nio.file.Files.readString(p);
            }
        } catch (Exception e) {
            // Ignore
        }
        return null;
    }

    /**
     * 执行系统命令
     */
    private String executeCommand(String... command) {
        try {
            ProcessBuilder pb = new ProcessBuilder(command);
            pb.redirectErrorStream(true);
            Process process = pb.start();
            String output = new String(process.getInputStream().readAllBytes());
            process.waitFor();
            return output.trim();
        } catch (Exception e) {
            logger.debug("Command failed: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 根据进程ID获取进程名
     */
    private String getProcessName(int processId) {
        try {
            // 使用 Windows Tasklist 命令获取进程名
            ProcessBuilder pb = new ProcessBuilder("tasklist", "/FI", "PID eq " + processId, "/FO", "CSV", "/NH");
            Process process = pb.start();
            java.io.BufferedReader reader = new java.io.BufferedReader(
                    new java.io.InputStreamReader(process.getInputStream()));

            String line = reader.readLine();
            if (line != null && line.contains("\"")) {
                // CSV格式: "Image Name","PID","Session Name","Session#","Mem Usage"
                String[] parts = line.split("\",\"");
                if (parts.length > 0) {
                    String name = parts[0].replace("\"", "").trim();
                    if (!name.equals("Image Name") && !name.isEmpty()) {
                        return name;
                    }
                }
            }
            reader.close();
        } catch (Exception e) {
            logger.debug("Failed to get process name for PID {}: {}", processId, e.getMessage());
        }
        return "Unknown";
    }

    /**
     * 获取用户存在状态
     */
    private PresenceStatus getPresenceStatus() {
        if (IS_WINDOWS) {
            return getPresenceStatusWindows();
        } else if (IS_LINUX) {
            return getPresenceStatusLinux();
        } else {
            return PresenceStatus.UNKNOWN;
        }
    }

    /**
     * Windows: 获取用户存在状态
     */
    private PresenceStatus getPresenceStatusWindows() {
        try {
            Kernel32 kernel32 = Kernel32.INSTANCE;
            LASTINPUTINFO lastInput = new LASTINPUTINFO();

            if (kernel32.GetLastInputInfo(lastInput) != 0) {
                long currentTime = kernel32.GetTickCount64();
                long lastInputTime = lastInput.dwTime & 0xFFFFFFFFL; // 转为无符号
                long idleTimeMs = currentTime - lastInputTime;
                long idleTimeSeconds = idleTimeMs / 1000;

                if (idleTimeSeconds < 30) {
                    return PresenceStatus.ACTIVE;
                } else if (idleTimeSeconds < IDLE_THRESHOLD_SECONDS) {
                    return PresenceStatus.IDLE;
                } else {
                    return PresenceStatus.AWAY;
                }
            }
        } catch (Exception e) {
            logger.warn("Failed to get idle time: {}", e.getMessage());
        }
        return PresenceStatus.UNKNOWN;
    }

    /**
     * S5-1: Linux: 获取用户存在状态
     */
    private PresenceStatus getPresenceStatusLinux() {
        try {
            // 方法1: 使用 xprintidle (如果安装了)
            String idleMsStr = executeCommand("xprintidle");
            if (idleMsStr != null && !idleMsStr.isEmpty()) {
                try {
                    long idleMs = Long.parseLong(idleMsStr.trim());
                    long idleSeconds = idleMs / 1000;

                    if (idleSeconds < 30) {
                        return PresenceStatus.ACTIVE;
                    } else if (idleSeconds < IDLE_THRESHOLD_SECONDS) {
                        return PresenceStatus.IDLE;
                    } else {
                        return PresenceStatus.AWAY;
                    }
                } catch (NumberFormatException e) {
                    logger.debug("Failed to parse idle time: {}", idleMsStr);
                }
            }

            // 方法2: 读取 /proc/interrupts 或使用 xdotool
            // 检查鼠标键盘活动
            String windowName = executeCommand("xdotool", "getactivewindow", "getwindowname");
            if (windowName != null && !windowName.isEmpty() && !windowName.equals("N/A")) {
                // 窗口存在，说明用户最近有操作
                return PresenceStatus.ACTIVE;
            }

            // 方法3: 基于 X11 screensaver
            String screensaverStr = executeCommand("xscrnsaver", "1");
            if (screensaverStr != null) {
                // Screensaver active = away
                if (screensaverStr.contains("on")) {
                    return PresenceStatus.AWAY;
                }
            }

        } catch (Exception e) {
            logger.debug("Failed to get presence status on Linux: {}", e.getMessage());
        }
        return PresenceStatus.UNKNOWN;
    }

    /**
     * 分类应用类型
     */
    private AppType classifyApp(String processName, String title) {
        // 检查进程名和标题
        String searchText = processName + " " + title;

        if (containsAny(searchText, BROWSER_KEYWORDS)) {
            return AppType.BROWSER;
        }
        if (containsAny(searchText, DEV_KEYWORDS)) {
            return AppType.DEVELOPMENT;
        }
        if (containsAny(searchText, CHAT_KEYWORDS)) {
            return AppType.CHAT;
        }
        if (containsAny(searchText, PRODUCTIVITY_KEYWORDS)) {
            return AppType.PRODUCTIVITY;
        }
        if (containsAny(searchText, MEDIA_KEYWORDS)) {
            return AppType.MEDIA;
        }
        if (containsAny(searchText, SYSTEM_KEYWORDS)) {
            return AppType.SYSTEM;
        }

        return AppType.UNKNOWN;
    }

    /**
     * 检查文本是否包含关键词
     */
    private boolean containsAny(String text, Set<String> keywords) {
        for (String keyword : keywords) {
            if (text.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 获取默认感知
     */
    private Perception getDefaultPerception() {
        return new Perception(
                Instant.now(),
                null,
                new UserPerception(
                        new WindowInfo("Unknown", "Unknown", AppType.UNKNOWN, false),
                        PresenceStatus.UNKNOWN,
                        java.util.List.of(),
                        null,
                        java.util.List.of()
                ),
                null, null, null, null
        );
    }

    @Override
    public String name() {
        return "RealUserSensor";
    }
}
