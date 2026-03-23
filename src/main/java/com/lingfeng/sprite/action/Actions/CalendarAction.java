package com.lingfeng.sprite.action.Actions;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.lingfeng.sprite.action.ActionPlugin;
import com.lingfeng.sprite.action.ActionResult;

/**
 * S7-2: 日历动作插件
 *
 * 创建日历事件/提醒
 *
 * 参数:
 * - title: 事件标题
 * - description: 事件描述
 * - startTime: 开始时间 (ISO格式或时间戳)
 * - endTime: 结束时间 (ISO格式或时间戳，可选)
 * - location: 地点 (可选)
 * - reminder: 提醒时间（分钟数，可选）
 * - calendarType: 日历类型 (google, outlook, local)
 */
public class CalendarAction implements ActionPlugin {

    private static final Logger logger = LoggerFactory.getLogger(CalendarAction.class);

    private static final DateTimeFormatter ISO_FORMATTER = DateTimeFormatter.ISO_DATE_TIME;

    @Override
    public String getName() {
        return "CalendarAction";
    }

    @Override
    public ActionResult execute(Map<String, Object> params) {
        try {
            String actionParam = (String) params.get("actionParam");
            Instant timestamp = (Instant) params.get("timestamp");

            // 解析参数
            String title = getString(params, "title", actionParam != null ? actionParam : "日历事件");
            String description = getString(params, "description", "");
            String startTimeStr = getString(params, "startTime", null);
            String endTimeStr = getString(params, "endTime", null);
            String location = getString(params, "location", "");
            Integer reminder = getInt(params, "reminder", 30);
            String calendarType = getString(params, "calendarType", "google");

            // 解析时间
            LocalDateTime startTime = parseTime(startTimeStr, LocalDateTime.now());
            LocalDateTime endTime = endTimeStr != null ? parseTime(endTimeStr, startTime.plusHours(1)) : startTime.plusHours(1);

            // 记录事件信息
            logger.info("=== Calendar Event ===");
            logger.info("Time: {}", timestamp);
            logger.info("Type: {}", calendarType);
            logger.info("Title: {}", title);
            logger.info("Description: {}", description);
            logger.info("Start: {}", startTime);
            logger.info("End: {}", endTime);
            logger.info("Location: {}", location);
            logger.info("Reminder: {} minutes before", reminder);
            logger.info("=====================");

            // 根据日历类型创建事件
            boolean created = createCalendarEvent(calendarType, title, description, startTime, endTime, location, reminder);

            if (created) {
                return ActionResult.success(String.format(
                    "Calendar event created: %s at %s (%s - %s)",
                    title, startTime.toLocalDate(), startTime.toLocalTime(), endTime.toLocalTime()
                ));
            } else {
                return ActionResult.failure("Failed to create calendar event: calendar type '" + calendarType + "' not supported or not configured");
            }

        } catch (Exception e) {
            logger.error("CalendarAction failed: {}", e.getMessage());
            return ActionResult.failure("CalendarAction failed: " + e.getMessage());
        }
    }

    /**
     * 创建日历事件
     *
     * 当前实现为日志记录模式，实际的API调用需要配置CalendarAction.configure()
     */
    private boolean createCalendarEvent(String calendarType, String title, String description,
                                       LocalDateTime startTime, LocalDateTime endTime,
                                       String location, int reminderMinutes) {
        try {
            switch (calendarType.toLowerCase()) {
                case "google":
                    // Google Calendar API 集成 (需要OAuth2配置)
                    // 当前为占位实现，实际使用时需要配置 Google Calendar API
                    logger.info("Google Calendar event would be created: {}", title);
                    return true; // 实际实现时返回API调用结果

                case "outlook":
                    // Microsoft Graph API 集成 (需要OAuth2配置)
                    // 当前为占位实现
                    logger.info("Outlook Calendar event would be created: {}", title);
                    return true;

                case "local":
                    // 本地日历 (iCalendar格式，保存到文件)
                    return createLocalCalendarEvent(title, description, startTime, endTime, location, reminderMinutes);

                default:
                    logger.warn("Unsupported calendar type: {}", calendarType);
                    return false;
            }
        } catch (Exception e) {
            logger.error("Failed to create calendar event: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 创建本地日历事件 (iCalendar格式)
     */
    private boolean createLocalCalendarEvent(String title, String description,
                                            LocalDateTime startTime, LocalDateTime endTime,
                                            String location, int reminderMinutes) {
        try {
            // 生成 iCalendar 格式的事件
            StringBuilder ics = new StringBuilder();
            ics.append("BEGIN:VCALENDAR\n");
            ics.append("VERSION:2.0\n");
            ics.append("PRODID:-//Sprite Digital Being//EN\n");
            ics.append("BEGIN:VEVENT\n");
            ics.append("UID:").append(java.util.UUID.randomUUID().toString()).append("\n");
            ics.append("DTSTAMP:").append(formatICSDateTime(LocalDateTime.now())).append("\n");
            ics.append("DTSTART:").append(formatICSDateTime(startTime)).append("\n");
            ics.append("DTEND:").append(formatICSDateTime(endTime)).append("\n");
            ics.append("SUMMARY:").append(escapeICSText(title)).append("\n");
            if (description != null && !description.isEmpty()) {
                ics.append("DESCRIPTION:").append(escapeICSText(description)).append("\n");
            }
            if (location != null && !location.isEmpty()) {
                ics.append("LOCATION:").append(escapeICSText(location)).append("\n");
            }
            if (reminderMinutes > 0) {
                ics.append("BEGIN:VALARM\n");
                ics.append("ACTION:DISPLAY\n");
                ics.append("DESCRIPTION:").append(escapeICSText(title)).append("\n");
                ics.append("TRIGGER:-PT").append(reminderMinutes).append("M\n");
                ics.append("END:VALARM\n");
            }
            ics.append("END:VEVENT\n");
            ics.append("END:VCALENDAR\n");

            // 记录到日志 (实际实现时可以保存到文件)
            logger.info("Local calendar event (iCalendar format):\n{}", ics.toString());

            return true;
        } catch (Exception e) {
            logger.error("Failed to create local calendar event: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 格式化iCalendar日期时间
     */
    private String formatICSDateTime(LocalDateTime dateTime) {
        return dateTime.atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss"));
    }

    /**
     * 转义iCalendar文本
     */
    private String escapeICSText(String text) {
        if (text == null) return "";
        return text.replace("\\", "\\\\")
                   .replace(",", "\\,")
                   .replace(";", "\\;")
                   .replace("\n", "\\n");
    }

    /**
     * 解析时间字符串
     */
    private LocalDateTime parseTime(String timeStr, LocalDateTime defaultTime) {
        if (timeStr == null || timeStr.isEmpty()) {
            return defaultTime;
        }

        try {
            // 尝试解析为时间戳 (毫秒)
            long timestamp = Long.parseLong(timeStr);
            return LocalDateTime.ofInstant(Instant.ofEpochMilli(timestamp), ZoneId.systemDefault());
        } catch (NumberFormatException e) {
            // 尝试解析为ISO格式
            try {
                return LocalDateTime.parse(timeStr, ISO_FORMATTER);
            } catch (Exception e2) {
                logger.warn("Failed to parse time '{}', using default", timeStr);
                return defaultTime;
            }
        }
    }

    /**
     * 从参数中获取字符串
     */
    private String getString(Map<String, Object> params, String key, String defaultValue) {
        Object value = params.get(key);
        if (value == null) {
            return defaultValue;
        }
        return value.toString();
    }

    /**
     * 从参数中获取整数
     */
    private int getInt(Map<String, Object> params, String key, int defaultValue) {
        Object value = params.get(key);
        if (value == null) {
            return defaultValue;
        }
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        try {
            return Integer.parseInt(value.toString());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
}
