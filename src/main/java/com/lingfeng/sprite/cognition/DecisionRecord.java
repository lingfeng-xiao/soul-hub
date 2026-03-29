package com.lingfeng.sprite.cognition;

import java.time.Instant;

/**
 * DecisionRecord - 决策记录实体
 *
 * CG-002: 决策持久化 - DecisionRecord
 *
 * 用于将决策持久化到数据库：
 * - id: 主键
 * - rationaleId: 决策理由ID
 * - frameId: 关联的推理帧ID
 * - decisionId: 决策ID
 * - action: 执行的动作
 * - confidence: 置信度
 * - rationaleText: 理由文本
 * - expectedOutcome: 预期结果
 * - actualOutcome: 实际结果（后续更新）
 * - createdAt: 创建时间
 * - updatedAt: 更新时间
 */
public class DecisionRecord {

    private Long id;
    private String rationaleId;
    private String frameId;
    private String decisionId;
    private String intentDescription;
    private String action;
    private float confidence;
    private String rationaleText;
    private String expectedOutcome;
    private String actualOutcome;
    private Instant createdAt;
    private Instant updatedAt;

    public DecisionRecord() {
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public DecisionRecord(String rationaleId, String frameId, CandidateDecision decision, DecisionRationale rationale) {
        this();
        this.rationaleId = rationaleId;
        this.frameId = frameId;
        this.decisionId = decision != null ? decision.decisionId() : null;
        this.intentDescription = decision != null && decision.intent() != null ? decision.intent().description() : null;
        this.action = decision != null ? decision.action() : null;
        this.confidence = decision != null ? decision.confidence() : 0.0f;
        this.rationaleText = rationale != null ? rationale.rationaleText() : null;
        this.expectedOutcome = rationale != null ? rationale.expectedOutcome() : null;
    }

    // Getters and Setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getRationaleId() {
        return rationaleId;
    }

    public void setRationaleId(String rationaleId) {
        this.rationaleId = rationaleId;
    }

    public String getFrameId() {
        return frameId;
    }

    public void setFrameId(String frameId) {
        this.frameId = frameId;
    }

    public String getDecisionId() {
        return decisionId;
    }

    public void setDecisionId(String decisionId) {
        this.decisionId = decisionId;
    }

    public String getIntentDescription() {
        return intentDescription;
    }

    public void setIntentDescription(String intentDescription) {
        this.intentDescription = intentDescription;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public float getConfidence() {
        return confidence;
    }

    public void setConfidence(float confidence) {
        this.confidence = confidence;
    }

    public String getRationaleText() {
        return rationaleText;
    }

    public void setRationaleText(String rationaleText) {
        this.rationaleText = rationaleText;
    }

    public String getExpectedOutcome() {
        return expectedOutcome;
    }

    public void setExpectedOutcome(String expectedOutcome) {
        this.expectedOutcome = expectedOutcome;
    }

    public String getActualOutcome() {
        return actualOutcome;
    }

    public void setActualOutcome(String actualOutcome) {
        this.actualOutcome = actualOutcome;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    /**
     * 更新实际结果
     */
    public void updateActualOutcome(String actualOutcome) {
        this.actualOutcome = actualOutcome;
        this.updatedAt = Instant.now();
    }

    @Override
    public String toString() {
        return "DecisionRecord{" +
            "id=" + id +
            ", rationaleId='" + rationaleId + '\'' +
            ", frameId='" + frameId + '\'' +
            ", decisionId='" + decisionId + '\'' +
            ", action='" + action + '\'' +
            ", confidence=" + confidence +
            ", createdAt=" + createdAt +
            '}';
    }
}
