package com.lingfeng.sprite.domain.self;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * SelfAssessment - 自我评估
 *
 * 代表 Sprite 对自己的评估，包括优势、劣势、学习风格、反思历史等。
 * 这个评估会随着时间和经验而更新。
 *
 * 对应旧: SelfModel.Self.metacognition
 */
public final class SelfAssessment {

    /**
     * 评估记录
     */
    public record AssessmentEntry(
            Instant timestamp,
            String aspect,
            float score,
            String reasoning
    ) {}

    /**
     * 学习风格
     */
    public enum LearningStyle {
        VISUAL,      // 视觉学习
        AUDITORY,    // 听觉学习
        READING,     // 阅读学习
        KINESTHETIC  // 动觉学习
    }

    /**
     * 学习风格
     */
    private final LearningStyle learningStyle;

    /**
     * 优势列表
     */
    private final List<String> strengths;

    /**
     * 劣势列表
     */
    private final List<String> blindSpots;

    /**
     * 决策模式
     */
    private final List<String> decisionPatterns;

    /**
     * 评估历史
     */
    private final List<AssessmentEntry> assessmentHistory;

    /**
     * 最后评估时间
     */
    private final Instant lastAssessment;

    private SelfAssessment(Builder builder) {
        this.learningStyle = builder.learningStyle;
        this.strengths = List.copyOf(builder.strengths);
        this.blindSpots = List.copyOf(builder.blindSpots);
        this.decisionPatterns = List.copyOf(builder.decisionPatterns);
        this.assessmentHistory = List.copyOf(builder.assessmentHistory);
        this.lastAssessment = builder.lastAssessment;
    }

    /**
     * 创建默认评估
     */
    public static SelfAssessment createDefault() {
        return new SelfAssessment.Builder()
                .learningStyle(LearningStyle.READING)
                .strengths(new ArrayList<>())
                .blindSpots(new ArrayList<>())
                .decisionPatterns(new ArrayList<>())
                .assessmentHistory(new ArrayList<>())
                .lastAssessment(Instant.now())
                .build();
    }

    /**
     * 添加评估记录
     */
    public SelfAssessment addAssessment(String aspect, float score, String reasoning) {
        List<AssessmentEntry> newHistory = new ArrayList<>(this.assessmentHistory);
        newHistory.add(new AssessmentEntry(Instant.now(), aspect, score, reasoning));

        return new SelfAssessment.Builder()
                .learningStyle(this.learningStyle)
                .strengths(this.strengths)
                .blindSpots(this.blindSpots)
                .decisionPatterns(this.decisionPatterns)
                .assessmentHistory(newHistory)
                .lastAssessment(Instant.now())
                .build();
    }

    /**
     * 更新优势
     */
    public SelfAssessment withStrengths(List<String> strengths) {
        return new SelfAssessment.Builder()
                .learningStyle(this.learningStyle)
                .strengths(strengths)
                .blindSpots(this.blindSpots)
                .decisionPatterns(this.decisionPatterns)
                .assessmentHistory(this.assessmentHistory)
                .lastAssessment(Instant.now())
                .build();
    }

    /**
     * 更新劣势
     */
    public SelfAssessment withBlindSpots(List<String> blindSpots) {
        return new SelfAssessment.Builder()
                .learningStyle(this.learningStyle)
                .strengths(this.strengths)
                .blindSpots(blindSpots)
                .decisionPatterns(this.decisionPatterns)
                .assessmentHistory(this.assessmentHistory)
                .lastAssessment(Instant.now())
                .build();
    }

    // Getters
    public LearningStyle getLearningStyle() {
        return learningStyle;
    }

    public List<String> getStrengths() {
        return strengths;
    }

    public List<String> getBlindSpots() {
        return blindSpots;
    }

    public List<String> getDecisionPatterns() {
        return decisionPatterns;
    }

    public List<AssessmentEntry> getAssessmentHistory() {
        return assessmentHistory;
    }

    public Instant getLastAssessment() {
        return lastAssessment;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SelfAssessment that = (SelfAssessment) o;
        return learningStyle == that.learningStyle &&
                Objects.equals(strengths, that.strengths) &&
                Objects.equals(blindSpots, that.blindSpots);
    }

    @Override
    public int hashCode() {
        return Objects.hash(learningStyle, strengths, blindSpots);
    }

    @Override
    public String toString() {
        return "SelfAssessment{" +
                "learningStyle=" + learningStyle +
                ", strengths=" + strengths +
                ", blindSpots=" + blindSpots +
                ", lastAssessment=" + lastAssessment +
                '}';
    }

    public static Builder builder() {
        return new Builder();
    }

    public Builder with() {
        return new Builder()
                .learningStyle(this.learningStyle)
                .strengths(this.strengths)
                .blindSpots(this.blindSpots)
                .decisionPatterns(this.decisionPatterns)
                .assessmentHistory(this.assessmentHistory)
                .lastAssessment(this.lastAssessment);
    }

    public static final class Builder {
        private LearningStyle learningStyle = LearningStyle.READING;
        private List<String> strengths = new ArrayList<>();
        private List<String> blindSpots = new ArrayList<>();
        private List<String> decisionPatterns = new ArrayList<>();
        private List<AssessmentEntry> assessmentHistory = new ArrayList<>();
        private Instant lastAssessment = Instant.now();

        public Builder learningStyle(LearningStyle learningStyle) {
            this.learningStyle = learningStyle;
            return this;
        }

        public Builder strengths(List<String> strengths) {
            this.strengths = strengths != null ? new ArrayList<>(strengths) : new ArrayList<>();
            return this;
        }

        public Builder blindSpots(List<String> blindSpots) {
            this.blindSpots = blindSpots != null ? new ArrayList<>(blindSpots) : new ArrayList<>();
            return this;
        }

        public Builder decisionPatterns(List<String> decisionPatterns) {
            this.decisionPatterns = decisionPatterns != null ? new ArrayList<>(decisionPatterns) : new ArrayList<>();
            return this;
        }

        public Builder assessmentHistory(List<AssessmentEntry> assessmentHistory) {
            this.assessmentHistory = assessmentHistory != null ? new ArrayList<>(assessmentHistory) : new ArrayList<>();
            return this;
        }

        public Builder lastAssessment(Instant lastAssessment) {
            this.lastAssessment = lastAssessment;
            return this;
        }

        public SelfAssessment build() {
            return new SelfAssessment(this);
        }
    }
}
