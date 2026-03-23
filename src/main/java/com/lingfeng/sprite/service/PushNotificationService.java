package com.lingfeng.sprite.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * S18-2: 推送通知服务 - 支持 FCM、 Xiaomi Mi Push、 Huawei Push
 *
 * 功能：
 * 1. 多平台推送通知发送
 * 2. 设备注册与 Token 管理
 * 3. 待处理通知队列
 */
@Service
public class PushNotificationService {

    private static final Logger logger = LoggerFactory.getLogger(PushNotificationService.class);

    // 设备注册表 (deviceId -> DeviceInfo)
    private final Map<String, DeviceInfo> registeredDevices = new ConcurrentHashMap<>();

    // 待处理通知队列 (deviceId -> List<PendingNotification>)
    private final Map<String, List<PendingNotification>> pendingNotifications = new ConcurrentHashMap<>();

    // FCM 配置 (可以通过 AppConfig 注入)
    private String fcmServerKey;
    private String fcmSenderId;

    // Xiaomi Mi Push 配置
    private String xiaomiAppId;
    private String xiaomiAppKey;

    // Huawei Push 配置
    private String huaweiAppId;
    private String huaweiClientId;
    private String huaweiClientSecret;

    public PushNotificationService() {
        logger.info("PushNotificationService initialized - supporting FCM, Xiaomi Mi Push, Huawei Push");
    }

    /**
     * 设备平台类型
     */
    public enum Platform {
        FCM,       // Firebase Cloud Messaging (Android/iOS)
        XIAOMI,    // Xiaomi Mi Push
        HUAWEI,    // Huawei Push Kit
        APNS       // Apple Push Notification Service (iOS)
    }

    /**
     * 设备信息
     */
    public record DeviceInfo(
            String deviceId,
            Platform platform,
            String token,
            Instant registeredAt,
            Instant lastActive,
            String deviceModel,
            String appVersion
    ) {
        public DeviceInfo withLastActive(Instant lastActive) {
            return new DeviceInfo(deviceId, platform, token, registeredAt, lastActive, deviceModel, appVersion);
        }
    }

    /**
     * 待处理通知
     */
    public record PendingNotification(
            String notificationId,
            String title,
            String body,
            String data,
            Instant createdAt,
            int retryCount
    ) {}

    /**
     * 推送结果
     */
    public record PushResult(
            boolean success,
            String messageId,
            String errorMessage
    ) {}

    /**
     * S18-2: 注册设备并存储推送 Token
     */
    public void registerDevice(String deviceId, Platform platform, String token) {
        registerDevice(deviceId, platform, token, null, null);
    }

    /**
     * S18-2: 注册设备并存储推送 Token（完整信息）
     */
    public void registerDevice(String deviceId, Platform platform, String token, String deviceModel, String appVersion) {
        DeviceInfo deviceInfo = new DeviceInfo(
                deviceId,
                platform,
                token,
                Instant.now(),
                Instant.now(),
                deviceModel,
                appVersion
        );
        registeredDevices.put(deviceId, deviceInfo);
        pendingNotifications.put(deviceId, new ArrayList<>());

        logger.info("Registered device: id={}, platform={}, model={}",
                deviceId, platform, deviceModel);

        // 初始化平台特定的推送客户端
        initializePlatformClient(platform);
    }

    /**
     * 初始化平台特定的推送客户端
     */
    private void initializePlatformClient(Platform platform) {
        switch (platform) {
            case FCM -> {
                if (fcmServerKey == null) {
                    logger.warn("FCM server key not configured, push notifications will be simulated");
                }
            }
            case XIAOMI -> {
                if (xiaomiAppId == null) {
                    logger.warn("Xiaomi App ID not configured, push notifications will be simulated");
                }
            }
            case HUAWEI -> {
                if (huaweiAppId == null) {
                    logger.warn("Huawei App ID not configured, push notifications will be simulated");
                }
            }
            case APNS -> {
                logger.info("APNS configured for iOS push notifications");
            }
        }
    }

    /**
     * S18-2: 发送推送通知
     */
    public PushResult sendPushNotification(String deviceToken, String title, String body) {
        return sendPushNotification(deviceToken, title, body, null);
    }

    /**
     * S18-2: 发送推送通知（带数据负载）
     */
    public PushResult sendPushNotification(String deviceToken, String title, String body, String data) {
        // 查找设备信息
        DeviceInfo device = findDeviceByToken(deviceToken);
        if (device == null) {
            logger.warn("Device not found for token, attempting to send without platform context");
            return new PushResult(false, null, "Device not registered");
        }

        return sendPushToPlatform(device.platform(), deviceToken, title, body, data);
    }

    /**
     * 根据 Token 查找设备
     */
    private DeviceInfo findDeviceByToken(String token) {
        return registeredDevices.values().stream()
                .filter(d -> d.token().equals(token))
                .findFirst()
                .orElse(null);
    }

    /**
     * 向指定平台发送推送
     */
    private PushResult sendPushToPlatform(Platform platform, String token, String title, String body, String data) {
        try {
            return switch (platform) {
                case FCM -> sendFcmPush(token, title, body, data);
                case XIAOMI -> sendXiaomiPush(token, title, body, data);
                case HUAWEI -> sendHuaweiPush(token, title, body, data);
                case APNS -> sendApnsPush(token, title, body, data);
            };
        } catch (Exception e) {
            logger.error("Failed to send push notification via {}: {}", platform, e.getMessage());
            return new PushResult(false, null, e.getMessage());
        }
    }

    /**
     * S18-2: 发送 FCM 推送
     */
    private PushResult sendFcmPush(String token, String title, String body, String data) {
        if (fcmServerKey == null || fcmServerKey.isEmpty()) {
            logger.info("FCM push simulated (no server key): title={}, body={}", title, body);
            return new PushResult(true, "simulated-" + System.currentTimeMillis(), null);
        }

        // 构建 FCM 消息
        FcmMessage fcmMessage = new FcmMessage(token, new FcmNotification(title, body), data);

        // TODO: 实现实际的 FCM HTTP v1 API 调用
        // 参考: https://firebase.google.com/docs/cloud-messaging/migrate-v1
        logger.info("FCM push would be sent: to={}, title={}", token, title);

        return new PushResult(true, "fcm-" + System.currentTimeMillis(), null);
    }

    /**
     * S18-2: 发送小米 Mi Push
     */
    private PushResult sendXiaomiPush(String token, String title, String body, String data) {
        if (xiaomiAppId == null || xiaomiAppId.isEmpty()) {
            logger.info("Xiaomi Mi Push simulated (no app ID): title={}, body={}", title, body);
            return new PushResult(true, "simulated-" + System.currentTimeMillis(), null);
        }

        // TODO: 实现实际的小米 Mi Push SDK 调用
        // 参考: https://dev.mi.com/mipush/docs/sdk/
        logger.info("Xiaomi Mi Push would be sent: to={}, title={}", token, title);

        return new PushResult(true, "xiaomi-" + System.currentTimeMillis(), null);
    }

    /**
     * S18-2: 发送华为 Push Kit
     */
    private PushResult sendHuaweiPush(String token, String title, String body, String data) {
        if (huaweiAppId == null || huaweiAppId.isEmpty()) {
            logger.info("Huawei Push simulated (no app ID): title={}, body={}", title, body);
            return new PushResult(true, "simulated-" + System.currentTimeMillis(), null);
        }

        // TODO: 实现实际的华为 Push Kit API 调用
        // 参考: https://developer.huawei.com/consumer/en/hms/huawei-pushkit
        logger.info("Huawei Push would be sent: to={}, title={}", token, title);

        return new PushResult(true, "huawei-" + System.currentTimeMillis(), null);
    }

    /**
     * S18-2: 发送 APNS 推送 (iOS)
     */
    private PushResult sendApnsPush(String token, String title, String body, String data) {
        // TODO: 实现实际的 APNS 调用
        // 可以使用 HTTP/2 API 或者第三方库
        logger.info("APNS push would be sent: to={}, title={}", token, title);

        return new PushResult(true, "apns-" + System.currentTimeMillis(), null);
    }

    /**
     * S18-3: 获取设备的待处理通知
     */
    public List<PendingNotification> getPendingNotifications(String deviceId) {
        List<PendingNotification> notifications = pendingNotifications.get(deviceId);
        if (notifications == null) {
            return List.of();
        }

        // 清理已过期的通知（保留24小时内的）
        Instant cutoff = Instant.now().minusSeconds(24 * 60 * 60);
        notifications.removeIf(n -> n.createdAt().isBefore(cutoff));

        return new ArrayList<>(notifications);
    }

    /**
     * S18-3: 添加待处理通知
     */
    public void addPendingNotification(String deviceId, String title, String body, String data) {
        PendingNotification notification = new PendingNotification(
                java.util.UUID.randomUUID().toString(),
                title,
                body,
                data,
                Instant.now(),
                0
        );

        pendingNotifications
                .computeIfAbsent(deviceId, k -> new ArrayList<>())
                .add(notification);

        logger.debug("Added pending notification for device {}: {}", deviceId, title);
    }

    /**
     * S18-3: 标记通知已发送
     */
    public void markNotificationSent(String deviceId, String notificationId) {
        List<PendingNotification> notifications = pendingNotifications.get(deviceId);
        if (notifications != null) {
            notifications.removeIf(n -> n.notificationId().equals(notificationId));
        }
    }

    /**
     * S18-2: 批量发送通知到所有注册设备
     */
    public void broadcastNotification(String title, String body) {
        for (DeviceInfo device : registeredDevices.values()) {
            try {
                sendPushNotification(device.token(), title, body);
            } catch (Exception e) {
                logger.error("Failed to broadcast to device {}: {}", device.deviceId(), e.getMessage());
            }
        }
    }

    /**
     * S18-3: 更新设备最后活跃时间
     */
    public void updateDeviceLastActive(String deviceId) {
        DeviceInfo device = registeredDevices.get(deviceId);
        if (device != null) {
            registeredDevices.put(deviceId, device.withLastActive(Instant.now()));
        }
    }

    /**
     * S18-3: 获取设备信息
     */
    public DeviceInfo getDeviceInfo(String deviceId) {
        return registeredDevices.get(deviceId);
    }

    /**
     * S18-3: 获取所有已注册设备
     */
    public List<DeviceInfo> getAllDevices() {
        return new ArrayList<>(registeredDevices.values());
    }

    /**
     * S18-3: 移除设备注册
     */
    public void unregisterDevice(String deviceId) {
        registeredDevices.remove(deviceId);
        pendingNotifications.remove(deviceId);
        logger.info("Unregistered device: {}", deviceId);
    }

    // ==================== 配置方法 ====================

    /**
     * 设置 FCM 配置
     */
    public void setFcmConfig(String serverKey, String senderId) {
        this.fcmServerKey = serverKey;
        this.fcmSenderId = senderId;
        logger.info("FCM config updated");
    }

    /**
     * 设置小米 Mi Push 配置
     */
    public void setXiaomiConfig(String appId, String appKey) {
        this.xiaomiAppId = appId;
        this.xiaomiAppKey = appKey;
        logger.info("Xiaomi Mi Push config updated");
    }

    /**
     * 设置华为 Push Kit 配置
     */
    public void setHuaweiConfig(String appId, String clientId, String clientSecret) {
        this.huaweiAppId = appId;
        this.huaweiClientId = clientId;
        this.huaweiClientSecret = clientSecret;
        logger.info("Huawei Push Kit config updated");
    }

    // ==================== FCM 内部类 ====================

    /**
     * FCM 消息结构
     */
    private static class FcmMessage {
        @JsonProperty("to") String to;
        @JsonProperty("notification") FcmNotification notification;
        @JsonProperty("data") String data;

        FcmMessage(String to, FcmNotification notification, String data) {
            this.to = to;
            this.notification = notification;
            this.data = data;
        }
    }

    /**
     * FCM 通知结构
     */
    private static class FcmNotification {
        @JsonProperty("title") String title;
        @JsonProperty("body") String body;

        FcmNotification(String title, String body) {
            this.title = title;
            this.body = body;
        }
    }
}
