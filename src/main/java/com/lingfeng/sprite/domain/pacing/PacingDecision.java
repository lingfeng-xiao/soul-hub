package com.lingfeng.sprite.domain.pacing;

import java.time.Instant;

/**
 * PacingDecision - 节速决策
 *
 * 描述如何处理某个变化的决策结果。
 *
 * 对应 IGN-090
 */
public final class PacingDecision {

    private final String changeId;
    private final ChangeLayer layer;
    private final ApplyStrategy strategy;
    private final String reason;
    private final Instant decidedAt;
    private final String changeDescription;

    private PacingDecision(Builder builder) {
        this.changeId = builder.changeId;
        this.layer = builder.layer;
        this.strategy = builder.strategy;
        this.reason = builder.reason;
        this.decidedAt = builder.decidedAt;
        this.changeDescription = builder.changeDescription;
    }

    /**
     * 创建快速决策
     */
    public static PacingDecision immediate(String changeId, String description) {
        return new Builder()
                .changeId(changeId)
                .layer(ChangeLayer.FAST)
                .strategy(ApplyStrategy.IMMEDIATE)
                .reason("快速层变化 - 自动应用")
                .decidedAt(Instant.now())
                .changeDescription(description)
                .build();
    }

    /**
     * 创建需解释的决策
     */
    public static PacingDecision explain(String changeId, String description, String reason) {
        return new Builder()
                .changeId(changeId)
                .layer(ChangeLayer.MEDIUM)
                .strategy(ApplyStrategy.EXPLAIN_THEN_APPLY)
                .reason(reason)
                .decidedAt(Instant.now())
                .changeDescription(description)
                .build();
    }

    /**
     * 创建需确认的决策
     */
    public static PacingDecision confirm(String changeId, String description, String reason) {
        return new Builder()
                .changeId(changeId)
                .layer(ChangeLayer.SLOW)
                .strategy(ApplyStrategy.CONFIRM_THEN_APPLY)
                .reason(reason)
                .decidedAt(Instant.now())
                .changeDescription(description)
                .build();
    }

    // Getters
    public String getChangeId() { return changeId; }
    public ChangeLayer getLayer() { return layer; }
    public ApplyStrategy getStrategy() { return strategy; }
    public String getReason() { return reason; }
    public Instant getDecidedAt() { return decidedAt; }
    public String getChangeDescription() { return changeDescription; }

    public boolean isImmediate() { return strategy == ApplyStrategy.IMMEDIATE; }
    public boolean needsExplanation() { return strategy == ApplyStrategy.EXPLAIN_THEN_APPLY; }
    public boolean needsConfirmation() { return strategy == ApplyStrategy.CONFIRM_THEN_APPLY; }

    @Override
    public String toString() {
        return String.format(
                "PacingDecision{changeId=%s, layer=%s, strategy=%s, reason='%s'}",
                changeId, layer, strategy, reason
        );
    }

    public static Builder builder() { return new Builder(); }

    public static final class Builder {
        private String changeId = "";
        private ChangeLayer layer = ChangeLayer.FAST;
        private ApplyStrategy strategy = ApplyStrategy.IMMEDIATE;
        private String reason = "";
        private Instant decidedAt = Instant.now();
        private String changeDescription = "";

        public Builder changeId(String changeId) { this.changeId = changeId; return this; }
        public Builder layer(ChangeLayer layer) { this.layer = layer; return this; }
        public Builder strategy(ApplyStrategy strategy) { this.strategy = strategy; return this; }
        public Builder reason(String reason) { this.reason = reason; return this; }
        public Builder decidedAt(Instant decidedAt) { this.decidedAt = decidedAt; return this; }
        public Builder changeDescription(String changeDescription) { this.changeDescription = changeDescription; return this; }

        public PacingDecision build() { return new PacingDecision(this); }
    }
}
