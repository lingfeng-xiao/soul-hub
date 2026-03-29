package com.lingfeng.sprite.domain.self;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * SelfState - 动态自我状态
 *
 * 代表 Sprite 当前时刻的动态自我状态，包括当前关注焦点、情绪基线、能力状态等。
 * 这个状态会随时间变化，反映 Sprite 的"活着"的状态。
 *
 * 对应旧: SelfModel.Self 中的动态部分
 */
public final class SelfState {

    /**
     * 当前关注焦点
     */
    private final List<String> attentionFoci;

    /**
     * 情绪基线
     */
    private final float emotionalBaseline;

    /**
     * 当前能量等级 (0-1)
     */
    private final float energyLevel;

    /**
     * 当前一致性分数 (0-1)
     */
    private final float coherenceScore;

    /**
     * 最后更新状态时间
     */
    private final Instant lastUpdated;

    /**
     * 状态来源
     */
    private final String source;

    private SelfState(Builder builder) {
        this.attentionFoci = List.copyOf(builder.attentionFoci);
        this.emotionalBaseline = builder.emotionalBaseline;
        this.energyLevel = builder.energyLevel;
        this.coherenceScore = builder.coherenceScore;
        this.lastUpdated = builder.lastUpdated;
        this.source = builder.source;
    }

    /**
     * 创建默认状态
     */
    public static SelfState createDefault() {
        return new SelfState.Builder()
                .attentionFoci(new ArrayList<>())
                .emotionalBaseline(0.5f)
                .energyLevel(1.0f)
                .coherenceScore(1.0f)
                .lastUpdated(Instant.now())
                .source("INITIALIZATION")
                .build();
    }

    /**
     * 添加关注焦点
     */
    public SelfState withAddedFocus(String focus) {
        List<String> newFoci = new ArrayList<>(this.attentionFoci);
        if (!newFoci.contains(focus)) {
            newFoci.add(focus);
        }
        return new SelfState.Builder()
                .attentionFoci(newFoci)
                .emotionalBaseline(this.emotionalBaseline)
                .energyLevel(this.energyLevel)
                .coherenceScore(this.coherenceScore)
                .lastUpdated(Instant.now())
                .source("FOCUS_ADDED")
                .build();
    }

    /**
     * 移除关注焦点
     */
    public SelfState withRemovedFocus(String focus) {
        List<String> newFoci = new ArrayList<>(this.attentionFoci);
        newFoci.remove(focus);
        return new SelfState.Builder()
                .attentionFoci(newFoci)
                .emotionalBaseline(this.emotionalBaseline)
                .energyLevel(this.energyLevel)
                .coherenceScore(this.coherenceScore)
                .lastUpdated(Instant.now())
                .source("FOCUS_REMOVED")
                .build();
    }

    /**
     * 更新能量等级
     */
    public SelfState withEnergyLevel(float level) {
        return new SelfState.Builder()
                .attentionFoci(this.attentionFoci)
                .emotionalBaseline(this.emotionalBaseline)
                .energyLevel(Math.max(0f, Math.min(1f, level)))
                .coherenceScore(this.coherenceScore)
                .lastUpdated(Instant.now())
                .source("ENERGY_UPDATE")
                .build();
    }

    /**
     * 更新一致性分数
     */
    public SelfState withCoherenceScore(float score) {
        return new SelfState.Builder()
                .attentionFoci(this.attentionFoci)
                .emotionalBaseline(this.emotionalBaseline)
                .energyLevel(this.energyLevel)
                .coherenceScore(Math.max(0f, Math.min(1f, score)))
                .lastUpdated(Instant.now())
                .source("COHERENCE_UPDATE")
                .build();
    }

    // Getters
    public List<String> getAttentionFoci() {
        return attentionFoci;
    }

    public float getEmotionalBaseline() {
        return emotionalBaseline;
    }

    public float getEnergyLevel() {
        return energyLevel;
    }

    public float getCoherenceScore() {
        return coherenceScore;
    }

    public Instant getLastUpdated() {
        return lastUpdated;
    }

    public String getSource() {
        return source;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SelfState selfState = (SelfState) o;
        return Float.compare(selfState.emotionalBaseline, emotionalBaseline) == 0 &&
                Float.compare(selfState.energyLevel, energyLevel) == 0 &&
                Float.compare(selfState.coherenceScore, coherenceScore) == 0 &&
                Objects.equals(attentionFoci, selfState.attentionFoci);
    }

    @Override
    public int hashCode() {
        return Objects.hash(attentionFoci, emotionalBaseline, energyLevel, coherenceScore);
    }

    @Override
    public String toString() {
        return "SelfState{" +
                "attentionFoci=" + attentionFoci +
                ", emotionalBaseline=" + emotionalBaseline +
                ", energyLevel=" + energyLevel +
                ", coherenceScore=" + coherenceScore +
                ", lastUpdated=" + lastUpdated +
                '}';
    }

    public static Builder builder() {
        return new Builder();
    }

    public Builder with() {
        return new Builder()
                .attentionFoci(this.attentionFoci)
                .emotionalBaseline(this.emotionalBaseline)
                .energyLevel(this.energyLevel)
                .coherenceScore(this.coherenceScore)
                .lastUpdated(this.lastUpdated)
                .source(this.source);
    }

    public static final class Builder {
        private List<String> attentionFoci = new ArrayList<>();
        private float emotionalBaseline = 0.5f;
        private float energyLevel = 1.0f;
        private float coherenceScore = 1.0f;
        private Instant lastUpdated = Instant.now();
        private String source = "";

        public Builder attentionFoci(List<String> attentionFoci) {
            this.attentionFoci = attentionFoci != null ? new ArrayList<>(attentionFoci) : new ArrayList<>();
            return this;
        }

        public Builder emotionalBaseline(float emotionalBaseline) {
            this.emotionalBaseline = Math.max(0f, Math.min(1f, emotionalBaseline));
            return this;
        }

        public Builder energyLevel(float energyLevel) {
            this.energyLevel = Math.max(0f, Math.min(1f, energyLevel));
            return this;
        }

        public Builder coherenceScore(float coherenceScore) {
            this.coherenceScore = Math.max(0f, Math.min(1f, coherenceScore));
            return this;
        }

        public Builder lastUpdated(Instant lastUpdated) {
            this.lastUpdated = lastUpdated;
            return this;
        }

        public Builder source(String source) {
            this.source = source;
            return this;
        }

        public SelfState build() {
            return new SelfState(this);
        }
    }
}
