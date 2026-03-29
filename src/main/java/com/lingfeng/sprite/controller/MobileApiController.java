package com.lingfeng.sprite.controller;

import java.time.Instant;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.lingfeng.sprite.Sprite;
import com.lingfeng.sprite.service.EmotionHistoryService;
import com.lingfeng.sprite.service.EmotionHistoryService.EmotionRecord;
import com.lingfeng.sprite.OwnerModel.Mood;
import com.lingfeng.sprite.service.PushNotificationService;
import com.lingfeng.sprite.service.PushNotificationService.DeviceInfo;
import com.lingfeng.sprite.service.PushNotificationService.PendingNotification;
import com.lingfeng.sprite.service.PushNotificationService.Platform;
import com.lingfeng.sprite.service.PushNotificationService.PushResult;
import com.lingfeng.sprite.service.SpriteService;

/**
 * S18: 移动伴侣 API 控制器
 *
 * 提供移动伴侣应用所需的 REST API：
 * - S18-1: Sprite 状态查询
 * - S18-2: 推送通知管理
 * - S18-3: 伴侣应用状态同步
 * - S18-4: 移动端对话接口
 */
@RestController
@RequestMapping("/api/mobile")
public class MobileApiController {

    private static final Logger logger = LoggerFactory.getLogger(MobileApiController.class);

    private final SpriteService spriteService;
    private final PushNotificationService pushNotificationService;
    private final EmotionHistoryService emotionHistoryService;

    public MobileApiController(
            SpriteService spriteService,
            PushNotificationService pushNotificationService,
            EmotionHistoryService emotionHistoryService
    ) {
        this.spriteService = spriteService;
        this.pushNotificationService = pushNotificationService;
        this.emotionHistoryService = emotionHistoryService;
    }

    // ==================== S18-1: Sprite 状态查询 ====================

    /**
     * S18-1: GET /api/mobile/status - 获取 Sprite 当前状态（移动端优化）
     */
    @GetMapping("/status")
    public ResponseEntity<MobileStatus> getMobileStatus() {
        Sprite.State state = spriteService.getState();

        MobileStatus status = new MobileStatus(
                state.identity().identity().displayName(),
                state.identity().identity().emoji(),
                state.platform().name(),
                state.isRunning(),
                state.lastCycleTime(),
                state.memoryStatus() != null ? state.memoryStatus().sensoryStimuliCount() : 0,
                state.memoryStatus() != null ? state.memoryStatus().workingMemoryItems() : 0,
                state.memoryStatus() != null ? (state.memoryStatus().longTermStats().episodicCount + state.memoryStatus().longTermStats().semanticCount + state.memoryStatus().longTermStats().proceduralCount + state.memoryStatus().longTermStats().perceptiveCount) : 0,
                state.identity().evolutionLevel(),
                state.hasLlmSupport()
        );

        return ResponseEntity.ok(status);
    }

    /**
     * S18-1: 移动端状态 DTO
     */
    public record MobileStatus(
            String name,
            String emoji,
            String platform,
            boolean isActive,
            Instant lastCycleTime,
            int sensoryMemorySize,
            int workingMemorySize,
            int longTermMemorySize,
            int evolutionLevel,
            boolean hasLlmSupport
    ) {}

    /**
     * S18-1: GET /api/mobile/state - 获取完整 Sprite 状态
     */
    @GetMapping("/state")
    public ResponseEntity<Sprite.State> getFullState() {
        return ResponseEntity.ok(spriteService.getState());
    }

    /**
     * S18-1: GET /api/mobile/emotions - 获取主人当前情绪（移动端优化）
     */
    @GetMapping("/emotions")
    public ResponseEntity<MobileEmotionData> getMobileEmotions() {
        EmotionRecord current = emotionHistoryService.getCurrentEmotion();
        if (current == null) {
            return ResponseEntity.ok(new MobileEmotionData(
                    "NEUTRAL",
                    0.5f,
                    null,
                    "unknown",
                    Instant.now()
            ));
        }

        MobileEmotionData emotionData = new MobileEmotionData(
                current.mood().name(),
                current.intensity(),
                current.trigger(),
                getEmotionAdvice(current.mood()),
                current.timestamp()
        );

        return ResponseEntity.ok(emotionData);
    }

    /**
     * S18-1: 移动端情绪数据 DTO
     */
    public record MobileEmotionData(
            String mood,
            float intensity,
            String trigger,
            String advice,
            Instant timestamp
    ) {}

    /**
     * 根据情绪给出建议
     */
    private String getEmotionAdvice(Mood mood) {
        return switch (mood) {
            case HAPPY -> "主人心情很好，可以主动分享一些有趣的事情";
            case EXCITED -> "主人情绪高涨，适合讨论重要的事情";
            case GRATEFUL -> "主人心存感激，互动会更加积极";
            case CONFIDENT -> "主人状态不错，可以放心交流";
            case CALM -> "主人心情平静，是闲聊的好时机";
            case NEUTRAL -> "主人情绪平稳，保持正常互动即可";
            case CONFUSED -> "主人可能有些困惑，需要耐心解释";
            case TIRED -> "主人可能比较疲惫，简短交流为宜";
            case SAD -> "主人心情低落，需要更多关心";
            case ANXIOUS -> "主人有些焦虑，需要安抚";
            case FRUSTRATED -> "主人可能遇到了挫折，需要支持";
        };
    }

    // ==================== S18-2: 推送通知管理 ====================

    /**
     * S18-2: POST /api/mobile/device/register - 注册设备推送 Token
     */
    @PostMapping("/device/register")
    public ResponseEntity<DeviceRegistrationResponse> registerDevice(@RequestBody DeviceRegistrationRequest request) {
        pushNotificationService.registerDevice(
                request.deviceId(),
                request.platform(),
                request.token(),
                request.deviceModel(),
                request.appVersion()
        );

        logger.info("Mobile device registered: id={}, platform={}",
                request.deviceId(), request.platform());

        return ResponseEntity.ok(new DeviceRegistrationResponse(
                true,
                "Device registered successfully",
                Instant.now()
        ));
    }

    /**
     * S18-2: 设备注册请求
     */
    public record DeviceRegistrationRequest(
            String deviceId,
            Platform platform,
            String token,
            String deviceModel,
            String appVersion
    ) {}

    /**
     * S18-2: 设备注册响应
     */
    public record DeviceRegistrationResponse(
            boolean success,
            String message,
            Instant registeredAt
    ) {}

    /**
     * S18-2: DELETE /api/mobile/device/{deviceId} - 注销设备
     */
    @DeleteMapping("/device/{deviceId}")
    public ResponseEntity<Void> unregisterDevice(@PathVariable String deviceId) {
        pushNotificationService.unregisterDevice(deviceId);
        return ResponseEntity.ok().build();
    }

    /**
     * S18-2: GET /api/mobile/notifications - 获取待处理通知
     */
    @GetMapping("/notifications")
    public ResponseEntity<List<PendingNotification>> getPendingNotifications(
            @RequestParam String deviceId
    ) {
        List<PendingNotification> notifications = pushNotificationService.getPendingNotifications(deviceId);
        return ResponseEntity.ok(notifications);
    }

    /**
     * S18-2: POST /api/mobile/notifications/send - 发送推送通知
     */
    @PostMapping("/notifications/send")
    public ResponseEntity<PushResult> sendNotification(@RequestBody SendNotificationRequest request) {
        PushResult result = pushNotificationService.sendPushNotification(
                request.token(),
                request.title(),
                request.body(),
                request.data()
        );
        return ResponseEntity.ok(result);
    }

    /**
     * S18-2: 发送通知请求
     */
    public record SendNotificationRequest(
            String token,
            String title,
            String body,
            String data
    ) {}

    /**
     * S18-2: POST /api/mobile/notifications/broadcast - 广播通知到所有设备
     */
    @PostMapping("/notifications/broadcast")
    public ResponseEntity<BroadcastResult> broadcastNotification(@RequestBody BroadcastRequest request) {
        pushNotificationService.broadcastNotification(request.title(), request.body());

        List<DeviceInfo> devices = pushNotificationService.getAllDevices();
        return ResponseEntity.ok(new BroadcastResult(
                true,
                "Broadcast sent to " + devices.size() + " devices",
                devices.size()
        ));
    }

    /**
     * S18-2: 广播请求
     */
    public record BroadcastRequest(
            String title,
            String body
    ) {}

    /**
     * S18-2: 广播结果
     */
    public record BroadcastResult(
            boolean success,
            String message,
            int deviceCount
    ) {}

    // ==================== S18-3: 伴侣应用状态同步 ====================

    /**
     * S18-3: GET /api/mobile/sync - 获取同步数据
     */
    @GetMapping("/sync")
    public ResponseEntity<SyncData> getSyncData(@RequestParam String deviceId) {
        // 更新设备活跃时间
        pushNotificationService.updateDeviceLastActive(deviceId);

        Sprite.State state = spriteService.getState();
        EmotionRecord currentEmotion = emotionHistoryService.getCurrentEmotion();
        List<PendingNotification> notifications = pushNotificationService.getPendingNotifications(deviceId);

        SyncData syncData = new SyncData(
                state.identity().identity().displayName(),
                state.identity().identity().emoji(),
                state.isRunning(),
                state.lastCycleTime(),
                currentEmotion != null ? currentEmotion.mood().name() : "NEUTRAL",
                currentEmotion != null ? currentEmotion.intensity() : 0.5f,
                notifications.size(),
                Instant.now()
        );

        return ResponseEntity.ok(syncData);
    }

    /**
     * S18-3: 同步数据 DTO
     */
    public record SyncData(
            String spriteName,
            String spriteEmoji,
            boolean spriteActive,
            Instant lastActiveTime,
            String ownerMood,
            float moodIntensity,
            int pendingNotificationCount,
            Instant syncTimestamp
    ) {}

    /**
     * S18-3: POST /api/mobile/sync - 上报伴侣应用状态
     */
    @PostMapping("/sync")
    public ResponseEntity<SyncResponse> reportStatus(@RequestBody StatusReport report) {
        logger.info("Status report from device {}: appVersion={}, battery={}, connected={}",
                report.deviceId(), report.appVersion(), report.batteryLevel(), report.isConnected());

        // 更新设备活跃时间
        pushNotificationService.updateDeviceLastActive(report.deviceId());

        return ResponseEntity.ok(new SyncResponse(
                true,
                "Status synced",
                Instant.now()
        ));
    }

    /**
     * S18-3: 状态上报请求
     */
    public record StatusReport(
            String deviceId,
            String appVersion,
            int batteryLevel,
            boolean isConnected,
            Instant timestamp
    ) {}

    /**
     * S18-3: 同步响应
     */
    public record SyncResponse(
            boolean success,
            String message,
            Instant syncedAt
    ) {}

    /**
     * S18-3: GET /api/mobile/devices - 获取所有已注册设备
     */
    @GetMapping("/devices")
    public ResponseEntity<List<DeviceInfo>> getAllDevices() {
        return ResponseEntity.ok(pushNotificationService.getAllDevices());
    }

    /**
     * S18-3: GET /api/mobile/device/{deviceId} - 获取设备信息
     */
    @GetMapping("/device/{deviceId}")
    public ResponseEntity<DeviceInfo> getDeviceInfo(@PathVariable String deviceId) {
        DeviceInfo device = pushNotificationService.getDeviceInfo(deviceId);
        if (device == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(device);
    }

    // ==================== S18-4: 移动端对话接口 ====================

    /**
     * S18-4: POST /api/mobile/message - 从移动端发送消息
     */
    @PostMapping("/message")
    public ResponseEntity<MobileMessageResponse> sendMessage(@RequestBody MobileMessageRequest request) {
        logger.info("Mobile message from session {}: {}", request.sessionId(), request.message());

        // TODO: 集成 ConversationService 处理消息
        // 目前返回占位响应，后续需要调用实际的对话处理逻辑

        MobileMessageResponse response = new MobileMessageResponse(
                "消息已收到，我会尽快回复！",
                true,
                Instant.now(),
                List.of()
        );

        return ResponseEntity.ok(response);
    }

    /**
     * S18-4: 移动端消息请求
     */
    public record MobileMessageRequest(
            String sessionId,
            String message,
            String deviceId
    ) {}

    /**
     * S18-4: 移动端消息响应
     */
    public record MobileMessageResponse(
            String response,
            boolean success,
            Instant timestamp,
            List<String> actions
    ) {}

    /**
     * S18-4: GET /api/mobile/conversation/history - 获取对话历史
     */
    @GetMapping("/conversation/history")
    public ResponseEntity<List<ConversationMessage>> getConversationHistory(
            @RequestParam String sessionId,
            @RequestParam(defaultValue = "50") int limit
    ) {
        // TODO: 从 ConversationService 获取实际对话历史
        // 目前返回空列表，后续需要实现

        return ResponseEntity.ok(List.of());
    }

    /**
     * S18-4: 对话消息 DTO
     */
    public record ConversationMessage(
            String role,
            String content,
            Instant timestamp
    ) {}

    /**
     * S18-4: POST /api/mobile/conversation/clear - 清除对话历史
     */
    @PostMapping("/conversation/clear")
    public ResponseEntity<Void> clearConversation(@RequestParam String sessionId) {
        logger.info("Clearing conversation history for session: {}", sessionId);
        // TODO: 调用 ConversationService 清除历史
        return ResponseEntity.ok().build();
    }
}
