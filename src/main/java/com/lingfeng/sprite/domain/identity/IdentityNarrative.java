package com.lingfeng.sprite.domain.identity;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * IdentityNarrative - 身份叙事
 *
 * 代表 Sprite 的身份叙事链，用于维护"我是谁"的连续性故事。
 * 每次身份发生重要变化时，都会生成新的叙事片段。
 *
 * 对应旧: SelfModel 中的 continuityChain 扩展
 */
public final class IdentityNarrative {

    /**
     * 叙事片段记录
     */
    public record NarrativeSegment(
            Instant timestamp,
            String narrative,
            String trigger,
            String context
    ) {}

    /**
     * 当前叙事摘要
     */
    private final String currentNarrative;

    /**
     * 叙事片段历史
     */
    private final List<NarrativeSegment> segments;

    /**
     * 叙事版本号
     */
    private final int version;

    /**
     * 最后更新时间
     */
    private final Instant lastUpdated;

    private IdentityNarrative(Builder builder) {
        this.currentNarrative = builder.currentNarrative;
        this.segments = List.copyOf(builder.segments);
        this.version = builder.version;
        this.lastUpdated = builder.lastUpdated;
    }

    /**
     * 创建初始叙事
     */
    public static IdentityNarrative createInitial(String narrative) {
        List<NarrativeSegment> initialSegments = new ArrayList<>();
        initialSegments.add(new NarrativeSegment(
                Instant.now(),
                narrative,
                "CREATION",
                "Sprite 首次创建"
        ));

        return new IdentityNarrative.Builder()
                .currentNarrative(narrative)
                .segments(initialSegments)
                .version(1)
                .lastUpdated(Instant.now())
                .build();
    }

    /**
     * 添加新的叙事片段
     */
    public IdentityNarrative appendSegment(String narrative, String trigger, String context) {
        List<NarrativeSegment> newSegments = new ArrayList<>(this.segments);
        newSegments.add(new NarrativeSegment(
                Instant.now(),
                narrative,
                trigger,
                context
        ));

        return new IdentityNarrative.Builder()
                .currentNarrative(narrative)
                .segments(newSegments)
                .version(this.version + 1)
                .lastUpdated(Instant.now())
                .build();
    }

    /**
     * 获取叙事历史摘要
     */
    public String getNarrativeHistorySummary() {
        StringBuilder sb = new StringBuilder();
        sb.append("叙事版本: ").append(version).append("\n");
        for (NarrativeSegment segment : segments) {
            sb.append("[").append(segment.timestamp()).append("] ");
            sb.append(segment.trigger()).append(": ");
            sb.append(segment.narrative()).append("\n");
        }
        return sb.toString();
    }

    // Getters
    public String getCurrentNarrative() {
        return currentNarrative;
    }

    public List<NarrativeSegment> getSegments() {
        return segments;
    }

    public int getVersion() {
        return version;
    }

    public Instant getLastUpdated() {
        return lastUpdated;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        IdentityNarrative that = (IdentityNarrative) o;
        return version == that.version &&
                Objects.equals(currentNarrative, that.currentNarrative);
    }

    @Override
    public int hashCode() {
        return Objects.hash(currentNarrative, version);
    }

    @Override
    public String toString() {
        return "IdentityNarrative{" +
                "currentNarrative='" + currentNarrative + '\'' +
                ", version=" + version +
                ", segmentsCount=" + segments.size() +
                ", lastUpdated=" + lastUpdated +
                '}';
    }

    public static Builder builder() {
        return new Builder();
    }

    public Builder with() {
        return new Builder()
                .currentNarrative(this.currentNarrative)
                .segments(this.segments)
                .version(this.version)
                .lastUpdated(this.lastUpdated);
    }

    public static final class Builder {
        private String currentNarrative = "";
        private List<NarrativeSegment> segments = new ArrayList<>();
        private int version = 1;
        private Instant lastUpdated = Instant.now();

        public Builder currentNarrative(String currentNarrative) {
            this.currentNarrative = currentNarrative != null ? currentNarrative : "";
            return this;
        }

        public Builder segments(List<NarrativeSegment> segments) {
            this.segments = segments != null ? new ArrayList<>(segments) : new ArrayList<>();
            return this;
        }

        public Builder version(int version) {
            this.version = version;
            return this;
        }

        public Builder lastUpdated(Instant lastUpdated) {
            this.lastUpdated = lastUpdated;
            return this;
        }

        public IdentityNarrative build() {
            return new IdentityNarrative(this);
        }
    }
}
