package com.lingfeng.sprite.action;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.PriorityBlockingQueue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * 快速反应处理器 - S16: 快速反应通道
 *
 * 用于绕过完整认知循环处理简单/紧急动作
 */
@Service
public class QuickReactionHandler {

    private static final Logger logger = LoggerFactory.getLogger(QuickReactionHandler.class);

    // 简单查询模式 - 不需要完整认知
    private static final Set<String> SIMPLE_PATTERNS = Set.of(
        "时间", "几点了", "现在", "现在几", "天气", "date", "time", "weather",
        "今天几号", "今天是", "星期几", "什么时间", "几点"
    );

    // 紧急触发模式
    private static final Set<String> URGENT_PATTERNS = Set.of(
        "紧急", "help", "救命", "报警", "SOS", "救命啊", "帮帮我", "紧急情况",
        "危险", "快来", "求救", "求助"
    );

    // 紧急事件优先队列
    private final PriorityBlockingQueue<UrgentEvent> urgentEventQueue = new PriorityBlockingQueue<>(
        100,
        (e1, e2) -> Integer.compare(e2.priority(), e1.priority())
    );

    // 待反馈的异步动作
    private final Map<String, SimpleFuture<ActionResult>> pendingAsyncActions = new ConcurrentHashMap<>();

    // S16-3: 检查输入是否可绕过完整认知
    public boolean canBypass(String input) {
        if (input == null || input.isBlank()) {
            return false;
        }
        String normalized = input.trim().toLowerCase();

        // 检查简单查询模式
        for (String pattern : SIMPLE_PATTERNS) {
            if (normalized.contains(pattern.toLowerCase())) {
                logger.debug("Input matches simple pattern: {}", pattern);
                return true;
            }
        }

        return false;
    }

    // S16-2: 检查是否为紧急事件
    public boolean isUrgent(String input) {
        if (input == null || input.isBlank()) {
            return false;
        }
        String normalized = input.trim().toLowerCase();

        for (String pattern : URGENT_PATTERNS) {
            if (normalized.contains(pattern.toLowerCase())) {
                logger.info("Urgent pattern detected: {}", pattern);
                return true;
            }
        }

        return false;
    }

    // S16-3: 直接响应简单查询
    public ActionResult handleDirect(String input) {
        if (input == null || input.isBlank()) {
            return ActionResult.failure("Empty input");
        }

        String normalized = input.trim().toLowerCase();

        // 时间查询
        if (containsAny(normalized, "时间", "几点了", "现在", "date", "time", "几点", "什么时间")) {
            return handleTimeQuery(normalized);
        }

        // 天气查询
        if (containsAny(normalized, "天气", "weather")) {
            return handleWeatherQuery(normalized);
        }

        // 日期查询
        if (containsAny(normalized, "今天几号", "今天是", "星期几", "几号")) {
            return handleDateQuery(normalized);
        }

        // 默认不匹配
        return null;
    }

    private boolean containsAny(String input, String... patterns) {
        for (String pattern : patterns) {
            if (input.contains(pattern.toLowerCase())) {
                return true;
            }
        }
        return false;
    }

    private ActionResult handleTimeQuery(String input) {
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss");
        String timeStr = now.format(formatter);
        return ActionResult.success("现在是 " + timeStr, Map.of("time", timeStr, "type", "time_query"));
    }

    private ActionResult handleDateQuery(String input) {
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy年MM月dd日");
        DateTimeFormatter dayFormatter = DateTimeFormatter.ofPattern("EEEE");

        String dateStr = now.format(dateFormatter);
        String dayStr = now.format(dayFormatter);

        // 转换星期几为中文
        dayStr = switch (dayStr) {
            case "Monday" -> "星期一";
            case "Tuesday" -> "星期二";
            case "Wednesday" -> "星期三";
            case "Thursday" -> "星期四";
            case "Friday" -> "星期五";
            case "Saturday" -> "星期六";
            case "Sunday" -> "星期日";
            default -> dayStr;
        };

        return ActionResult.success(dateStr + " " + dayStr,
            Map.of("date", dateStr, "dayOfWeek", dayStr, "type", "date_query"));
    }

    private ActionResult handleWeatherQuery(String input) {
        // 简化实现，实际应调用天气API
        return ActionResult.success("抱歉，暂时无法获取天气信息，请稍后再试。",
            Map.of("type", "weather_query", "available", false));
    }

    // S16-2: 添加紧急事件到优先队列
    public void addUrgentEvent(String eventId, String content, int priority) {
        UrgentEvent event = new UrgentEvent(eventId, content, priority, System.currentTimeMillis());
        urgentEventQueue.offer(event);
        logger.info("Added urgent event to queue: id={}, priority={}, content={}",
            eventId, priority, content);
    }

    // S16-2: 获取下一个紧急事件
    public UrgentEvent pollUrgentEvent() {
        return urgentEventQueue.poll();
    }

    // S16-2: 获取队列大小
    public int getUrgentQueueSize() {
        return urgentEventQueue.size();
    }

    // S16-4: 注册异步动作
    public void registerAsyncAction(String actionId, SimpleFuture<ActionResult> future) {
        pendingAsyncActions.put(actionId, future);
        logger.debug("Registered async action: {}", actionId);
    }

    // S16-4: 获取异步动作结果
    public SimpleFuture<ActionResult> getAsyncActionResult(String actionId) {
        return pendingAsyncActions.remove(actionId);
    }

    // S16-4: 异步动作完成回调
    public void completeAsyncAction(String actionId, ActionResult result) {
        SimpleFuture<ActionResult> future = pendingAsyncActions.remove(actionId);
        if (future != null) {
            future.complete(result);
            logger.debug("Completed async action: {}", actionId);
        }
    }

    // 紧急事件记录
    public record UrgentEvent(
        String eventId,
        String content,
        int priority,
        long timestamp
    ) {}

    // 用于异步结果的简单CompletableFuture实现
    public static class SimpleFuture<T> {
        private T result;
        private boolean completed = false;
        private final Object lock = new Object();

        public boolean complete(T result) {
            synchronized (lock) {
                if (completed) return false;
                this.result = result;
                this.completed = true;
                lock.notifyAll();
                return true;
            }
        }

        public T get() {
            synchronized (lock) {
                while (!completed) {
                    try {
                        lock.wait();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        return null;
                    }
                }
                return result;
            }
        }

        public boolean isDone() {
            synchronized (lock) {
                return completed;
            }
        }
    }
}
