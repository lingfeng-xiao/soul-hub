package com.lingfeng.sprite.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.lingfeng.sprite.SelfModel;

/**
 * S31-1: Sprite协作服务 - Sprite发现协议
 *
 * 支持Sprite-to-Sprite的协作功能：
 * - S31-1: 广播存在（Presence Announcement Protocol）
 * - S31-1: 发现其他Sprite
 * - S31-2: 发起协作（Collaboration Session Establishment）
 * - S31-2: 分发任务（Task Distribution）
 *
 * 集成点：
 * - MultiDeviceCoordinationService: 设备间通信
 * - WebhookService: 事件通知
 */
@Service
public class SpriteCollaborationService {

    private static final Logger logger = LoggerFactory.getLogger(SpriteCollaborationService.class);

    private final MultiDeviceCoordinationService multiDeviceCoordinationService;
    private final WebhookService webhookService;

    // 本地Sprite信息
    private final String localSpriteId;
    private final String localSpriteName;
    private final String localSpriteVersion;
    private final String localEndpoint;
    private final List<SelfModel.Capability> localCapabilities;

    // 已发现的Sprite注册表
    private final Map<String, SpriteInfo> discoveredSprites = new ConcurrentHashMap<>();

    // 活跃的协作会话
    private final Map<String, CollaborationSession> activeSessions = new ConcurrentHashMap<>();

    // 待分发的任务队列
    private final List<Task> pendingTasks = new CopyOnWriteArrayList<>();

    // 发现超时时间（毫秒）
    private static final long DISCOVERY_TIMEOUT_MS = 30000;

    // Presence广播间隔（毫秒）
    private static final long PRESENCE_BROADCAST_INTERVAL_MS = 10000;

    // 最后广播时间
    private volatile long lastBroadcastTime = 0;

    /**
     * S31-1: Sprite信息
     */
    public record SpriteInfo(
        String id,
        String name,
        List<CapabilityInfo> capabilities,
        String version,
        String endpoint,
        Instant lastSeen,
        SpriteState state,
        float compatibilityScore
    ) {
        public SpriteInfo {
            if (id == null) id = "";
            if (name == null) name = "Unknown";
            if (capabilities == null) capabilities = List.of();
            if (version == null) version = "1.0";
            if (endpoint == null) endpoint = "";
            if (lastSeen == null) lastSeen = Instant.now();
            if (state == null) state = SpriteState.UNKNOWN;
        }

        public SpriteInfo withLastSeen(Instant time) {
            return new SpriteInfo(id, name, capabilities, version, endpoint, time, state, compatibilityScore);
        }

        public SpriteInfo withState(SpriteState newState) {
            return new SpriteInfo(id, name, capabilities, version, endpoint, lastSeen, newState, compatibilityScore);
        }

        public SpriteInfo withCompatibilityScore(float score) {
            return new SpriteInfo(id, name, capabilities, version, endpoint, lastSeen, state, score);
        }

        /**
         * 能力信息
         */
        public record CapabilityInfo(
            String name,
            String level,
            float confidence
        ) {}
    }

    /**
     * Sprite状态
     */
    public enum SpriteState {
        AVAILABLE,     // 可用，可协作
        BUSY,          // 忙碌
        UNREACHABLE,   // 不可达
        UNKNOWN        // 未知
    }

    /**
     * S31-2: 协作会话
     */
    public record CollaborationSession(
        String id,
        List<String> participants,
        SessionStatus status,
        Map<String, Object> sharedContext,
        Instant createdAt,
        Instant lastActivity,
        List<String> taskIds
    ) {
        public CollaborationSession {
            if (id == null) id = "";
            if (participants == null) participants = List.of();
            if (status == null) status = SessionStatus.PENDING;
            if (sharedContext == null) sharedContext = Map.of();
            if (createdAt == null) createdAt = Instant.now();
            if (lastActivity == null) lastActivity = Instant.now();
            if (taskIds == null) taskIds = List.of();
        }

        public CollaborationSession withStatus(SessionStatus newStatus) {
            return new CollaborationSession(id, participants, newStatus, sharedContext, createdAt, Instant.now(), taskIds);
        }

        public CollaborationSession withLastActivity(Instant time) {
            return new CollaborationSession(id, participants, status, sharedContext, createdAt, time, taskIds);
        }

        public CollaborationSession withTaskId(String taskId) {
            List<String> newTaskIds = new ArrayList<>(taskIds);
            newTaskIds.add(taskId);
            return new CollaborationSession(id, participants, status, sharedContext, createdAt, lastActivity, newTaskIds);
        }
    }

    /**
     * 会话状态
     */
    public enum SessionStatus {
        PENDING,        // 待确认
        ACTIVE,        // 活跃
        SUSPENDED,     // 暂停
        COMPLETED,     // 完成
        FAILED         // 失败
    }

    /**
     * S31-2: 任务
     */
    public record Task(
        String id,
        TaskType type,
        Map<String, Object> payload,
        String assignedTo,
        TaskStatus status,
        Instant createdAt,
        Instant completedAt,
        String result
    ) {
        public Task {
            if (id == null) id = "";
            if (type == null) type = TaskType.GENERAL;
            if (payload == null) payload = Map.of();
            if (assignedTo == null) assignedTo = "";
            if (status == null) status = TaskStatus.PENDING;
            if (createdAt == null) createdAt = Instant.now();
        }

        public Task withStatus(TaskStatus newStatus) {
            return new Task(id, type, payload, assignedTo, newStatus, createdAt,
                newStatus == TaskStatus.COMPLETED || newStatus == TaskStatus.FAILED ? Instant.now() : completedAt,
                result);
        }

        public Task withResult(String newResult) {
            return new Task(id, type, payload, assignedTo, status, createdAt, completedAt, newResult);
        }
    }

    /**
     * 任务类型
     */
    public enum TaskType {
        GENERAL,           // 一般任务
        ANALYSIS,          // 分析任务
        COORDINATION,      // 协调任务
        EXECUTION,         // 执行任务
        MONITORING         // 监控任务
    }

    /**
     * 任务状态
     */
    public enum TaskStatus {
        PENDING,       // 待处理
        ASSIGNED,      // 已分配
        IN_PROGRESS,   // 进行中
        COMPLETED,     // 完成
        FAILED,        // 失败
        CANCELLED      // 取消
    }

    /**
     * 能力协商请求
     */
    public record CapabilityNegotiationRequest(
        String requestId,
        String spriteId,
        List<SpriteInfo.CapabilityInfo> offeredCapabilities,
        List<String> desiredCapabilities,
        Instant timestamp
    ) {}

    /**
     * 能力协商响应
     */
    public record CapabilityNegotiationResponse(
        String requestId,
        String spriteId,
        boolean accepted,
        List<SpriteInfo.CapabilityInfo> matchedCapabilities,
        String reason
    ) {}

    public SpriteCollaborationService(
            MultiDeviceCoordinationService multiDeviceCoordinationService,
            WebhookService webhookService) {
        this.multiDeviceCoordinationService = multiDeviceCoordinationService;
        this.webhookService = webhookService;

        // 初始化本地Sprite信息（使用默认值，后续可以通过配置注入）
        this.localSpriteId = "sprite-" + System.currentTimeMillis();
        this.localSpriteName = "LocalSprite";
        this.localSpriteVersion = "1.0";
        this.localEndpoint = "http://localhost:8080";
        this.localCapabilities = List.of(
            new SelfModel.Capability("communication", SelfModel.CapabilityLevel.ADVANCED, 0.9f),
            new SelfModel.Capability("analysis", SelfModel.CapabilityLevel.BASIC, 0.7f),
            new SelfModel.Capability("coordination", SelfModel.CapabilityLevel.MASTER, 0.95f)
        );

        logger.info("SpriteCollaborationService initialized with local sprite id: {}", localSpriteId);
    }

    // ==================== S31-1: Presence Announcement ====================

    /**
     * S31-1: 广播本地Sprite存在
     *
     * 向所有在线设备广播本地Sprite的存在和能力
     */
    public void broadcastPresence() {
        long now = System.currentTimeMillis();
        if (now - lastBroadcastTime < PRESENCE_BROADCAST_INTERVAL_MS) {
            logger.debug("Presence broadcast skipped (too recent)");
            return;
        }
        lastBroadcastTime = now;

        PresenceAnnouncement announcement = buildPresenceAnnouncement();

        String message = serializePresenceAnnouncement(announcement);
        multiDeviceCoordinationService.broadcast(
            MultiDeviceCoordinationService.MessageType.NOTIFICATION,
            message
        );

        logger.debug("Presence broadcast sent: spriteId={}, capabilities={}",
            announcement.spriteId(), announcement.capabilities().size());

        // 触发Webhook事件
        webhookService.triggerEvent(WebhookService.EventType.OWNER_INTERACTION,
            Map.of("event", "PRESENCE_BROADCAST", "spriteId", localSpriteId));
    }

    /**
     * S31-1: 广播本地Sprite存在（带设备ID）
     */
    public void broadcastPresence(String deviceId) {
        PresenceAnnouncement announcement = buildPresenceAnnouncement();

        String message = serializePresenceAnnouncement(announcement);
        multiDeviceCoordinationService.sendMessage(
            deviceId,
            MultiDeviceCoordinationService.MessageType.NOTIFICATION,
            message
        );

        logger.debug("Presence sent to device: spriteId={}, targetDevice={}",
            announcement.spriteId(), deviceId);
    }

    /**
     * 构建存在声明
     */
    private PresenceAnnouncement buildPresenceAnnouncement() {
        List<SpriteInfo.CapabilityInfo> caps = localCapabilities.stream()
            .map(c -> new SpriteInfo.CapabilityInfo(c.name(), c.level().name(), c.confidence()))
            .collect(Collectors.toList());

        return new PresenceAnnouncement(
            localSpriteId,
            localSpriteName,
            caps,
            localSpriteVersion,
            localEndpoint,
            Instant.now()
        );
    }

    /**
     * 存在声明
     */
    private record PresenceAnnouncement(
        String spriteId,
        String spriteName,
        List<SpriteInfo.CapabilityInfo> capabilities,
        String version,
        String endpoint,
        Instant timestamp
    ) {}

    /**
     * 序列化存在声明
     */
    private String serializePresenceAnnouncement(PresenceAnnouncement announcement) {
        StringBuilder sb = new StringBuilder();
        sb.append("PRESENCE:");
        sb.append(announcement.spriteId()).append("|");
        sb.append(announcement.spriteName()).append("|");
        sb.append(announcement.version()).append("|");
        sb.append(announcement.endpoint()).append("|");
        sb.append(announcement.timestamp().toString()).append("|");
        sb.append(announcement.capabilities().stream()
            .map(c -> c.name() + ":" + c.level() + ":" + c.confidence())
            .collect(Collectors.joining(",")));
        return sb.toString();
    }

    /**
     * 反序列化存在声明
     */
    private PresenceAnnouncement deserializePresenceAnnouncement(String message) {
        try {
            if (!message.startsWith("PRESENCE:")) {
                return null;
            }
            String[] parts = message.substring(9).split("\\|");
            if (parts.length < 6) {
                return null;
            }

            String[] capParts = parts[5].split(",");
            List<SpriteInfo.CapabilityInfo> capabilities = new ArrayList<>();
            for (String cap : capParts) {
                String[] capInfo = cap.split(":");
                if (capInfo.length >= 3) {
                    capabilities.add(new SpriteInfo.CapabilityInfo(
                        capInfo[0],
                        capInfo[1],
                        Float.parseFloat(capInfo[2])
                    ));
                }
            }

            return new PresenceAnnouncement(
                parts[0],  // spriteId
                parts[1],  // spriteName
                capabilities,
                parts[2],  // version
                parts[3],  // endpoint
                Instant.parse(parts[4])  // timestamp
            );
        } catch (Exception e) {
            logger.warn("Failed to deserialize presence announcement: {}", e.getMessage());
            return null;
        }
    }

    // ==================== S31-1: Sprite Discovery ====================

    /**
     * S31-1: 发现其他Sprite
     *
     * 从已接收的存在声明中获取Sprite列表
     *
     * @return 发现的Sprite信息列表
     */
    public List<SpriteInfo> discoverSprites() {
        // 清理超时的Sprite
        cleanupTimedOutSprites();

        // 获取消息历史中的Presence消息
        List<MultiDeviceCoordinationService.CoordinationMessage> messages =
            multiDeviceCoordinationService.getMessageHistory();

        for (MultiDeviceCoordinationService.CoordinationMessage msg : messages) {
            if (msg.type() == MultiDeviceCoordinationService.MessageType.NOTIFICATION
                && msg.content().startsWith("PRESENCE:")) {
                processPresenceAnnouncement(msg.content(), msg.sourceDeviceId());
            }
        }

        List<SpriteInfo> result = new ArrayList<>(discoveredSprites.values());
        logger.debug("Discovered {} sprites", result.size());

        return result;
    }

    /**
     * S31-1: 发现指定设备上的Sprite
     */
    public List<SpriteInfo> discoverSpritesFromDevice(String deviceId) {
        List<MultiDeviceCoordinationService.CoordinationMessage> messages =
            multiDeviceCoordinationService.getMessagesForDevice(deviceId);

        for (MultiDeviceCoordinationService.CoordinationMessage msg : messages) {
            if (msg.type() == MultiDeviceCoordinationService.MessageType.NOTIFICATION
                && msg.content().startsWith("PRESENCE:")) {
                processPresenceAnnouncement(msg.content(), msg.sourceDeviceId());
            }
        }

        return discoveredSprites.values().stream()
            .filter(s -> s.state() == SpriteState.AVAILABLE)
            .collect(Collectors.toList());
    }

    /**
     * 处理存在声明
     */
    private void processPresenceAnnouncement(String message, String sourceDeviceId) {
        PresenceAnnouncement announcement = deserializePresenceAnnouncement(message);
        if (announcement == null) {
            return;
        }

        // 排除自己
        if (announcement.spriteId().equals(localSpriteId)) {
            return;
        }

        // 计算兼容性分数
        float compatibilityScore = calculateCompatibilityScore(
            announcement.capabilities(), localCapabilities);

        SpriteInfo spriteInfo = new SpriteInfo(
            announcement.spriteId(),
            announcement.spriteName(),
            announcement.capabilities(),
            announcement.version(),
            announcement.endpoint(),
            announcement.timestamp(),
            SpriteState.AVAILABLE,
            compatibilityScore
        );

        discoveredSprites.put(announcement.spriteId(), spriteInfo);
        logger.debug("Processed presence announcement: spriteId={}, compatibility={}",
            announcement.spriteId(), compatibilityScore);
    }

    /**
     * 计算兼容性分数
     */
    private float calculateCompatibilityScore(
            List<SpriteInfo.CapabilityInfo> remote,
            List<SelfModel.Capability> local) {

        if (remote == null || local == null || remote.isEmpty() || local.isEmpty()) {
            return 0.5f;
        }

        int matchCount = 0;
        for (SpriteInfo.CapabilityInfo remoteCap : remote) {
            for (SelfModel.Capability localCap : local) {
                if (remoteCap.name().equals(localCap.name())) {
                    // 能力名称匹配
                    matchCount++;
                    break;
                }
            }
        }

        return (float) matchCount / Math.max(remote.size(), local.size());
    }

    /**
     * 清理超时的Sprite
     */
    private void cleanupTimedOutSprites() {
        long now = System.currentTimeMillis();
        discoveredSprites.entrySet().removeIf(entry -> {
            boolean timedOut = now - entry.getValue().lastSeen().toEpochMilli() > DISCOVERY_TIMEOUT_MS;
            if (timedOut) {
                logger.debug("Sprite timed out: {}", entry.getKey());
            }
            return timedOut;
        });
    }

    /**
     * S31-1: 获取Sprite信息
     */
    public SpriteInfo getSprite(String spriteId) {
        SpriteInfo sprite = discoveredSprites.get(spriteId);
        if (sprite != null && isSpriteTimedOut(sprite)) {
            discoveredSprites.remove(spriteId);
            return null;
        }
        return sprite;
    }

    /**
     * 检查Sprite是否超时
     */
    private boolean isSpriteTimedOut(SpriteInfo sprite) {
        return System.currentTimeMillis() - sprite.lastSeen().toEpochMilli() > DISCOVERY_TIMEOUT_MS;
    }

    // ==================== S31-2: Collaboration Session ====================

    /**
     * S31-2: 发起协作
     *
     * @param targetSpriteId 目标Sprite ID
     * @return 协作会话，如果失败返回null
     */
    public CollaborationSession startCollaboration(String targetSpriteId) {
        SpriteInfo targetSprite = discoveredSprites.get(targetSpriteId);
        if (targetSprite == null) {
            logger.warn("Cannot start collaboration: target sprite not found - {}", targetSpriteId);
            return null;
        }

        if (targetSprite.state() != SpriteState.AVAILABLE) {
            logger.warn("Cannot start collaboration: target sprite not available - {} ({})",
                targetSpriteId, targetSprite.state());
            return null;
        }

        // 创建协作会话
        String sessionId = "collab-" + System.currentTimeMillis() + "-" + Math.random();
        CollaborationSession session = new CollaborationSession(
            sessionId,
            List.of(localSpriteId, targetSpriteId),
            SessionStatus.PENDING,
            Map.of(
                "initiatedBy", localSpriteId,
                "targetSprite", targetSpriteId,
                "compatibilityScore", targetSprite.compatibilityScore()
            ),
            Instant.now(),
            Instant.now(),
            List.of()
        );

        activeSessions.put(sessionId, session);

        // 更新目标Sprite状态
        SpriteInfo updatedTarget = targetSprite.withState(SpriteState.BUSY);
        discoveredSprites.put(targetSpriteId, updatedTarget);

        logger.info("Collaboration session started: sessionId={}, participants={}",
            sessionId, session.participants());

        // 触发Webhook事件
        webhookService.triggerEvent(WebhookService.EventType.OWNER_INTERACTION,
            Map.of("event", "COLLABORATION_STARTED",
                "sessionId", sessionId,
                "targetSprite", targetSpriteId));

        return session;
    }

    /**
     * S31-2: 接受协作邀请
     */
    public CollaborationSession acceptCollaboration(String sessionId) {
        CollaborationSession session = activeSessions.get(sessionId);
        if (session == null) {
            logger.warn("Cannot accept collaboration: session not found - {}", sessionId);
            return null;
        }

        CollaborationSession updatedSession = session.withStatus(SessionStatus.ACTIVE);
        activeSessions.put(sessionId, updatedSession);

        logger.info("Collaboration accepted: sessionId={}", sessionId);

        return updatedSession;
    }

    /**
     * S31-2: 拒绝协作邀请
     */
    public boolean rejectCollaboration(String sessionId) {
        CollaborationSession session = activeSessions.get(sessionId);
        if (session == null) {
            logger.warn("Cannot reject collaboration: session not found - {}", sessionId);
            return false;
        }

        CollaborationSession updatedSession = session.withStatus(SessionStatus.FAILED);
        activeSessions.put(sessionId, updatedSession);

        // 释放目标Sprite
        if (session.participants().size() > 1) {
            String targetSpriteId = session.participants().get(1);
            SpriteInfo targetSprite = discoveredSprites.get(targetSpriteId);
            if (targetSprite != null) {
                discoveredSprites.put(targetSpriteId, targetSprite.withState(SpriteState.AVAILABLE));
            }
        }

        logger.info("Collaboration rejected: sessionId={}", sessionId);

        return true;
    }

    /**
     * S31-2: 结束协作会话
     */
    public boolean endCollaboration(String sessionId) {
        CollaborationSession session = activeSessions.get(sessionId);
        if (session == null) {
            logger.warn("Cannot end collaboration: session not found - {}", sessionId);
            return false;
        }

        CollaborationSession updatedSession = session.withStatus(SessionStatus.COMPLETED);
        activeSessions.put(sessionId, updatedSession);

        // 释放所有参与Sprite
        for (String spriteId : session.participants()) {
            if (!spriteId.equals(localSpriteId)) {
                SpriteInfo sprite = discoveredSprites.get(spriteId);
                if (sprite != null) {
                    discoveredSprites.put(spriteId, sprite.withState(SpriteState.AVAILABLE));
                }
            }
        }

        logger.info("Collaboration ended: sessionId={}", sessionId);

        return true;
    }

    /**
     * S31-2: 获取活跃会话
     */
    public List<CollaborationSession> getActiveSessions() {
        return activeSessions.values().stream()
            .filter(s -> s.status() == SessionStatus.ACTIVE || s.status() == SessionStatus.PENDING)
            .collect(Collectors.toList());
    }

    /**
     * S31-2: 获取会话
     */
    public CollaborationSession getSession(String sessionId) {
        return activeSessions.get(sessionId);
    }

    // ==================== S31-2: Task Distribution ====================

    /**
     * S31-2: 分发任务
     *
     * @param session 协作会话
     * @param task 要分发的任务
     */
    public void distributeTask(CollaborationSession session, Task task) {
        if (session == null) {
            logger.warn("Cannot distribute task: session is null");
            return;
        }

        if (session.status() != SessionStatus.ACTIVE) {
            logger.warn("Cannot distribute task: session not active - {} ({})",
                session.id(), session.status());
            return;
        }

        // 创建任务
        String taskId = task.id() != null && !task.id().isEmpty() ? task.id() : "task-" + System.currentTimeMillis();
        Task distributedTask = new Task(
            taskId,
            task.type(),
            task.payload(),
            task.assignedTo(),
            TaskStatus.ASSIGNED,
            Instant.now(),
            null,
            null
        );

        // 添加到待分发队列
        pendingTasks.add(distributedTask);

        // 更新会话
        CollaborationSession updatedSession = session.withTaskId(taskId);
        activeSessions.put(session.id(), updatedSession);

        logger.info("Task distributed: taskId={}, sessionId={}, assignedTo={}",
            taskId, session.id(), task.assignedTo());

        // 触发Webhook事件
        webhookService.triggerEvent(WebhookService.EventType.ACTION_EXECUTED,
            Map.of("event", "TASK_DISTRIBUTED",
                "taskId", taskId,
                "sessionId", session.id(),
                "type", task.type().name()));
    }

    /**
     * S31-2: 创建并分发任务
     */
    public Task createAndDistributeTask(CollaborationSession session, TaskType type,
            Map<String, Object> payload, String assignedTo) {
        if (session == null) {
            logger.warn("Cannot create and distribute task: session is null");
            return null;
        }

        Task task = new Task(
            "",
            type,
            payload,
            assignedTo,
            TaskStatus.PENDING,
            Instant.now(),
            null,
            null
        );

        distributeTask(session, task);
        return task;
    }

    /**
     * S31-2: 获取待处理任务
     */
    public List<Task> getPendingTasks() {
        return new ArrayList<>(pendingTasks);
    }

    /**
     * S31-2: 获取指定会话的任务
     */
    public List<Task> getTasksForSession(String sessionId) {
        CollaborationSession session = activeSessions.get(sessionId);
        if (session == null) {
            return List.of();
        }

        return pendingTasks.stream()
            .filter(t -> session.taskIds().contains(t.id()))
            .collect(Collectors.toList());
    }

    /**
     * S31-2: 更新任务状态
     */
    public Task updateTaskStatus(String taskId, TaskStatus newStatus, String result) {
        for (int i = 0; i < pendingTasks.size(); i++) {
            Task task = pendingTasks.get(i);
            if (task.id().equals(taskId)) {
                Task updatedTask = task
                    .withStatus(newStatus)
                    .withResult(result);
                pendingTasks.set(i, updatedTask);

                logger.debug("Task status updated: taskId={}, status={}", taskId, newStatus);

                return updatedTask;
            }
        }
        return null;
    }

    /**
     * S31-2: 完成任务
     */
    public Task completeTask(String taskId, String result) {
        return updateTaskStatus(taskId, TaskStatus.COMPLETED, result);
    }

    /**
     * S31-2: 取消任务
     */
    public Task cancelTask(String taskId) {
        return updateTaskStatus(taskId, TaskStatus.CANCELLED, null);
    }

    // ==================== S31-2: Capability Negotiation ====================

    /**
     * S31-2: 请求能力协商
     */
    public CapabilityNegotiationResponse requestCapabilityNegotiation(
            String targetSpriteId,
            List<String> desiredCapabilities) {

        SpriteInfo targetSprite = discoveredSprites.get(targetSpriteId);
        if (targetSprite == null) {
            logger.warn("Cannot negotiate: target sprite not found - {}", targetSpriteId);
            return new CapabilityNegotiationResponse(
                "",
                targetSpriteId,
                false,
                List.of(),
                "Target sprite not found"
            );
        }

        String requestId = "neg-" + System.currentTimeMillis();

        // 构建协商请求
        List<SpriteInfo.CapabilityInfo> offeredCapabilities = localCapabilities.stream()
            .map(c -> new SpriteInfo.CapabilityInfo(c.name(), c.level().name(), c.confidence()))
            .collect(Collectors.toList());

        CapabilityNegotiationRequest request = new CapabilityNegotiationRequest(
            requestId,
            localSpriteId,
            offeredCapabilities,
            desiredCapabilities,
            Instant.now()
        );

        // 找到匹配的能力
        List<SpriteInfo.CapabilityInfo> matchedCapabilities = new ArrayList<>();
        for (String desired : desiredCapabilities) {
            for (SpriteInfo.CapabilityInfo offered : offeredCapabilities) {
                if (offered.name().equals(desired)) {
                    matchedCapabilities.add(offered);
                    break;
                }
            }
        }

        boolean accepted = !matchedCapabilities.isEmpty();
        String reason = accepted ? "Capabilities matched" : "No matching capabilities found";

        logger.info("Capability negotiation: requestId={}, target={}, accepted={}, matched={}",
            requestId, targetSpriteId, accepted, matchedCapabilities.size());

        return new CapabilityNegotiationResponse(
            requestId,
            targetSpriteId,
            accepted,
            matchedCapabilities,
            reason
        );
    }

    // ==================== Utility Methods ====================

    /**
     * 获取协作服务状态
     */
    public CollaborationStatus getStatus() {
        int discoveredCount = discoveredSprites.size();
        int activeSessionCount = (int) activeSessions.values().stream()
            .filter(s -> s.status() == SessionStatus.ACTIVE)
            .count();
        int pendingTaskCount = pendingTasks.size();

        return new CollaborationStatus(
            localSpriteId,
            discoveredCount,
            activeSessionCount,
            pendingTaskCount,
            Instant.now()
        );
    }

    /**
     * 协作状态
     */
    public record CollaborationStatus(
        String localSpriteId,
        int discoveredSpritesCount,
        int activeSessionsCount,
        int pendingTasksCount,
        Instant timestamp
    ) {}

    /**
     * 重置发现列表
     */
    public void resetDiscovery() {
        discoveredSprites.clear();
        logger.info("Sprite discovery list reset");
    }

    /**
     * 设置本地Sprite信息
     */
    public void setLocalSpriteInfo(String id, String name, String version, String endpoint,
            List<SelfModel.Capability> capabilities) {
        // Note: This would require more careful implementation for production
        // For now, we log the requested update
        logger.info("Local sprite info update requested: id={}, name={}, version={}",
            id, name, version);
    }
}
