package com.lingfeng.sprite.domain.snapshot;

import java.time.Instant;

/**
 * PacingState - 进化节速状态
 *
 * 表示 Sprite 当前进化节速的状态，用于告知主人在不同层面的变化速度。
 */
public final class PacingState {

    /**
     * 节速层级
     */
    public enum PacingLayer {
        FAST,    // 快速层 - 自动应用
        MEDIUM,  // 中速层 - 解释后应用
        SLOW     // 慢速层 - 确认后应用
    }

    /**
     * 当前活跃层级状态
     */
    public enum LayerStatus {
        STABLE,        // 稳定
        EVOLVING,      // 进化中
        PENDING_SYNC   // 待同步
    }

    private final PacingLayer currentLayer;
    private final LayerStatus status;
    private final int pendingChangesCount;
    private final int recentChangesCount;
    private final Instant lastSyncTime;
    private final String syncRecommendation;

    private PacingState(Builder builder) {
        this.currentLayer = builder.currentLayer;
        this.status = builder.status;
        this.pendingChangesCount = builder.pendingChangesCount;
        this.recentChangesCount = builder.recentChangesCount;
        this.lastSyncTime = builder.lastSyncTime;
        this.syncRecommendation = builder.syncRecommendation;
    }

    public static PacingState createDefault() {
        return new Builder()
                .currentLayer(PacingLayer.FAST)
                .status(LayerStatus.STABLE)
                .pendingChangesCount(0)
                .recentChangesCount(0)
                .lastSyncTime(Instant.now())
                .syncRecommendation("运行正常")
                .build();
    }

    public static PacingState fromEvolution(PacingLayer layer, int pendingCount, int recentCount) {
        LayerStatus status = pendingCount > 0 ? LayerStatus.PENDING_SYNC :
                           recentCount > 3 ? LayerStatus.EVOLVING : LayerStatus.STABLE;

        String recommendation = switch (status) {
            case PENDING_SYNC -> "有待同步的变化，建议进行关系同步";
            case EVOLVING -> "近期变化较多，建议关注变化说明";
            case STABLE -> "运行正常";
        };

        return new Builder()
                .currentLayer(layer)
                .status(status)
                .pendingChangesCount(pendingCount)
                .recentChangesCount(recentCount)
                .lastSyncTime(Instant.now())
                .syncRecommendation(recommendation)
                .build();
    }

    // Getters
    public PacingLayer getCurrentLayer() { return currentLayer; }
    public LayerStatus getStatus() { return status; }
    public int getPendingChangesCount() { return pendingChangesCount; }
    public int getRecentChangesCount() { return recentChangesCount; }
    public Instant getLastSyncTime() { return lastSyncTime; }
    public String getSyncRecommendation() { return syncRecommendation; }

    public static Builder builder() { return new Builder(); }

    public static final class Builder {
        private PacingLayer currentLayer = PacingLayer.FAST;
        private LayerStatus status = LayerStatus.STABLE;
        private int pendingChangesCount = 0;
        private int recentChangesCount = 0;
        private Instant lastSyncTime = Instant.now();
        private String syncRecommendation = "";

        public Builder currentLayer(PacingLayer currentLayer) { this.currentLayer = currentLayer; return this; }
        public Builder status(LayerStatus status) { this.status = status; return this; }
        public Builder pendingChangesCount(int pendingChangesCount) { this.pendingChangesCount = pendingChangesCount; return this; }
        public Builder recentChangesCount(int recentChangesCount) { this.recentChangesCount = recentChangesCount; return this; }
        public Builder lastSyncTime(Instant lastSyncTime) { this.lastSyncTime = lastSyncTime; return this; }
        public Builder syncRecommendation(String syncRecommendation) { this.syncRecommendation = syncRecommendation; return this; }

        public PacingState build() { return new PacingState(this); }
    }
}
