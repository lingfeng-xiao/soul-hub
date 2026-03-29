package com.lingfeng.sprite.feedback;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * FeedbackCollector - 反馈收集器
 */
public class FeedbackCollector {

    private final ConcurrentHashMap<String, FeedbackEvent> feedbackStore = new ConcurrentHashMap<>();

    /**
     * 收集反馈事件
     */
    public void collect(FeedbackEvent event) {
        feedbackStore.put(event.eventId(), event);
    }

    /**
     * 获取最近的反馈
     */
    public List<FeedbackEvent> getRecentFeedback(int limit) {
        return feedbackStore.values().stream()
                .sorted((e1, e2) -> e2.timestamp().compareTo(e1.timestamp()))
                .limit(limit)
                .collect(Collectors.toList());
    }

    /**
     * 根据周期ID获取反馈
     */
    public List<FeedbackEvent> getFeedbackByCycle(String cycleId) {
        return feedbackStore.values().stream()
                .filter(e -> cycleId.equals(e.targetCycleId()))
                .sorted((e1, e2) -> e2.timestamp().compareTo(e1.timestamp()))
                .collect(Collectors.toList());
    }

    /**
     * 根据来源获取反馈
     */
    public List<FeedbackEvent> getFeedbackBySource(FeedbackEvent.FeedbackSource source) {
        return feedbackStore.values().stream()
                .filter(e -> source.equals(e.source()))
                .sorted((e1, e2) -> e2.timestamp().compareTo(e1.timestamp()))
                .collect(Collectors.toList());
    }

    /**
     * 根据ID获取反馈
     */
    public FeedbackEvent getById(String eventId) {
        return feedbackStore.get(eventId);
    }

    /**
     * 获取所有反馈
     */
    public List<FeedbackEvent> getAllFeedback() {
        return new ArrayList<>(feedbackStore.values());
    }

    /**
     * 清除所有反馈
     */
    public void clear() {
        feedbackStore.clear();
    }
}
