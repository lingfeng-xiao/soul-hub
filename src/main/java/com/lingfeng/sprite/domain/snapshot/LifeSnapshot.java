package com.lingfeng.sprite.domain.snapshot;

import com.lingfeng.sprite.domain.self.SelfState;
import com.lingfeng.sprite.domain.self.AttentionFocus;
import com.lingfeng.sprite.domain.goal.ActiveIntention;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * LifeSnapshot - 生命快照
 *
 * Sprite 当前存在状态的统一表达。
 * 这是前后端共享的核心数据结构，用于展示 Sprite 的"现在感"。
 *
 * 对应 IGN-010
 */
public final class LifeSnapshot {

    /**
     * 快照版本
     */
    private final String version;

    /**
     * 生成时间
     */
    private final Instant generatedAt;

    /**
     * 身份摘要
     */
    private final String identitySummary;

    /**
     * 自我状态
     */
    private final SelfState currentState;

    /**
     * 当前注意力焦点
     */
    private final AttentionFocus attentionFocus;

    /**
     * 当前活跃意向列表
     */
    private final List<ActiveIntention> activeIntentions;

    /**
     * 关系摘要
     */
    private final RelationshipSummary relationshipSummary;

    /**
     * 最近变化列表
     */
    private final List<RecentChange> recentChanges;

    /**
     * 最近重要记忆摘要
     */
    private final List<String> recentMemorySummaries;

    /**
     * 接下来可能的行动
     */
    private final List<String> nextLikelyActions;

    /**
     * 一致性分数 (0-1)
     */
    private final float coherenceScore;

    /**
     * 进化节速状态
     */
    private final PacingState pacingState;

    /**
     * 身份 Emoji
     */
    private final String emoji;

    /**
     * 身份名称
     */
    private final String displayName;

    private LifeSnapshot(Builder builder) {
        this.version = builder.version;
        this.generatedAt = builder.generatedAt;
        this.identitySummary = builder.identitySummary;
        this.currentState = builder.currentState;
        this.attentionFocus = builder.attentionFocus;
        this.activeIntentions = List.copyOf(builder.activeIntentions);
        this.relationshipSummary = builder.relationshipSummary;
        this.recentChanges = List.copyOf(builder.recentChanges);
        this.recentMemorySummaries = List.copyOf(builder.recentMemorySummaries);
        this.nextLikelyActions = List.copyOf(builder.nextLikelyActions);
        this.coherenceScore = builder.coherenceScore;
        this.pacingState = builder.pacingState;
        this.emoji = builder.emoji;
        this.displayName = builder.displayName;
    }

    /**
     * 创建默认快照
     */
    public static LifeSnapshot createDefault() {
        return new Builder()
                .version("1.0")
                .generatedAt(Instant.now())
                .identitySummary("正在初始化...")
                .currentState(SelfState.createDefault())
                .attentionFocus(AttentionFocus.idle())
                .activeIntentions(new ArrayList<>())
                .relationshipSummary(RelationshipSummary.createDefault())
                .recentChanges(new ArrayList<>())
                .recentMemorySummaries(new ArrayList<>())
                .nextLikelyActions(new ArrayList<>())
                .coherenceScore(0.5f)
                .pacingState(PacingState.createDefault())
                .emoji("✨")
                .displayName("Sprite")
                .build();
    }

    // Getters
    public String getVersion() { return version; }
    public Instant getGeneratedAt() { return generatedAt; }
    public String getIdentitySummary() { return identitySummary; }
    public SelfState getCurrentState() { return currentState; }
    public AttentionFocus getAttentionFocus() { return attentionFocus; }
    public List<ActiveIntention> getActiveIntentions() { return activeIntentions; }
    public RelationshipSummary getRelationshipSummary() { return relationshipSummary; }
    public List<RecentChange> getRecentChanges() { return recentChanges; }
    public List<String> getRecentMemorySummaries() { return recentMemorySummaries; }
    public List<String> getNextLikelyActions() { return nextLikelyActions; }
    public float getCoherenceScore() { return coherenceScore; }
    public PacingState getPacingState() { return pacingState; }
    public String getEmoji() { return emoji; }
    public String getDisplayName() { return displayName; }

    @Override
    public String toString() {
        return String.format(
                "LifeSnapshot{emoji=%s, displayName=%s, coherence=%.0f%%, intentions=%d, pacing=%s}",
                emoji, displayName, coherenceScore * 100, activeIntentions.size(), pacingState.getStatus()
        );
    }

    public static Builder builder() { return new Builder(); }

    public static final class Builder {
        private String version = "1.0";
        private Instant generatedAt = Instant.now();
        private String identitySummary = "";
        private SelfState currentState = null;
        private AttentionFocus attentionFocus = null;
        private List<ActiveIntention> activeIntentions = new ArrayList<>();
        private RelationshipSummary relationshipSummary = RelationshipSummary.createDefault();
        private List<RecentChange> recentChanges = new ArrayList<>();
        private List<String> recentMemorySummaries = new ArrayList<>();
        private List<String> nextLikelyActions = new ArrayList<>();
        private float coherenceScore = 0.5f;
        private PacingState pacingState = PacingState.createDefault();
        private String emoji = "✨";
        private String displayName = "Sprite";

        public Builder version(String version) { this.version = version; return this; }
        public Builder generatedAt(Instant generatedAt) { this.generatedAt = generatedAt; return this; }
        public Builder identitySummary(String identitySummary) { this.identitySummary = identitySummary; return this; }
        public Builder currentState(SelfState currentState) { this.currentState = currentState; return this; }
        public Builder attentionFocus(AttentionFocus attentionFocus) { this.attentionFocus = attentionFocus; return this; }
        public Builder activeIntentions(List<ActiveIntention> activeIntentions) { this.activeIntentions = activeIntentions; return this; }
        public Builder relationshipSummary(RelationshipSummary relationshipSummary) { this.relationshipSummary = relationshipSummary; return this; }
        public Builder recentChanges(List<RecentChange> recentChanges) { this.recentChanges = recentChanges; return this; }
        public Builder recentMemorySummaries(List<String> recentMemorySummaries) { this.recentMemorySummaries = recentMemorySummaries; return this; }
        public Builder nextLikelyActions(List<String> nextLikelyActions) { this.nextLikelyActions = nextLikelyActions; return this; }
        public Builder coherenceScore(float coherenceScore) { this.coherenceScore = coherenceScore; return this; }
        public Builder pacingState(PacingState pacingState) { this.pacingState = pacingState; return this; }
        public Builder emoji(String emoji) { this.emoji = emoji; return this; }
        public Builder displayName(String displayName) { this.displayName = displayName; return this; }

        public LifeSnapshot build() { return new LifeSnapshot(this); }
    }
}
