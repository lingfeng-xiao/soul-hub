package com.lingfeng.sprite.domain.self;

import com.lingfeng.sprite.domain.identity.IdentityAnchor;
import com.lingfeng.sprite.domain.identity.IdentityNarrative;
import com.lingfeng.sprite.domain.identity.IdentityProfile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;

/**
 * SelfService - 自我核心服务
 *
 * 提供自我状态的统一管理，包括状态更新、注意力焦点管理、自我评估等。
 *
 * 对应旧: SelfModel 和 SelfLearningService 的部分逻辑
 */
@Service
public final class SelfService {

    private static final Logger logger = LoggerFactory.getLogger(SelfService.class);

    private SelfState currentState;
    private SelfAssessment assessment;
    private AttentionFocus currentFocus;
    private BoundaryProfile boundaries;

    public SelfService() {
        this.currentState = SelfState.createDefault();
        this.assessment = SelfAssessment.createDefault();
        this.currentFocus = AttentionFocus.idle();
        this.boundaries = BoundaryProfile.createDefault();
    }

    /**
     * 获取当前状态
     */
    public SelfState getCurrentState() {
        return currentState;
    }

    /**
     * 获取当前焦点
     */
    public AttentionFocus getCurrentFocus() {
        return currentFocus;
    }

    /**
     * 获取自我评估
     */
    public SelfAssessment getAssessment() {
        return assessment;
    }

    /**
     * 获取边界配置
     */
    public BoundaryProfile getBoundaries() {
        return boundaries;
    }

    /**
     * Replace the full self state from a persisted projection.
     */
    public void replaceState(
            SelfState currentState,
            SelfAssessment assessment,
            AttentionFocus currentFocus,
            BoundaryProfile boundaries
    ) {
        this.currentState = currentState != null ? currentState : SelfState.createDefault();
        this.assessment = assessment != null ? assessment : SelfAssessment.createDefault();
        this.currentFocus = currentFocus != null ? currentFocus : AttentionFocus.idle();
        this.boundaries = boundaries != null ? boundaries : BoundaryProfile.createDefault();
    }

    /**
     * Reset self state to defaults.
     */
    public void reset() {
        this.currentState = SelfState.createDefault();
        this.assessment = SelfAssessment.createDefault();
        this.currentFocus = AttentionFocus.idle();
        this.boundaries = BoundaryProfile.createDefault();
    }

    /**
     * 更新自我状态
     */
    public void updateState(SelfState newState) {
        logger.debug("Self state updated: coherence={}, energy={}",
                newState.getCoherenceScore(), newState.getEnergyLevel());
        this.currentState = newState;
    }

    /**
     * 更新注意力焦点
     */
    public void updateFocus(AttentionFocus focus) {
        logger.debug("Attention focus updated: type={}, description={}",
                focus.getType(), focus.getDescription());
        this.currentFocus = focus;
    }

    /**
     * 添加关注焦点
     */
    public void addFocus(String focusDescription) {
        this.currentState = currentState.withAddedFocus(focusDescription);
    }

    /**
     * 清除焦点
     */
    public void clearFocus() {
        this.currentFocus = AttentionFocus.idle();
    }

    /**
     * 检查焦点是否超时
     */
    public boolean isFocusTimedOut() {
        return currentFocus.isTimedOut();
    }

    /**
     * 更新评估
     */
    public void updateAssessment(SelfAssessment newAssessment) {
        logger.debug("Self assessment updated: strengths={}, blindSpots={}",
                newAssessment.getStrengths().size(), newAssessment.getBlindSpots().size());
        this.assessment = newAssessment;
    }

    /**
     * 更新边界配置
     */
    public void updateBoundaries(BoundaryProfile newBoundaries) {
        logger.info("Boundaries updated by: {}", newBoundaries.getLastModifiedBy());
        this.boundaries = newBoundaries;
    }

    /**
     * 检查行动是否在边界内
     */
    public boolean isActionWithinBoundaries(String actionDescription, BoundaryProfile.BoundaryType type) {
        return boundaries.isAllowed(actionDescription, type);
    }

    /**
     * 获取自我总结
     */
    public String getSelfSummary() {
        return String.format(
                "当前状态: %s | 能量: %.0f%% | 一致性: %.0f%% | 焦点: %s",
                currentFocus.getType(),
                currentState.getEnergyLevel() * 100,
                currentState.getCoherenceScore() * 100,
                currentFocus.getDescription()
        );
    }

    /**
     * 降低能量
     */
    public void drainEnergy(float amount) {
        float newEnergy = Math.max(0, currentState.getEnergyLevel() - amount);
        this.currentState = currentState.withEnergyLevel(newEnergy);
        logger.debug("Energy drained to: {:.0f}%", newEnergy * 100);
    }

    /**
     * 恢复能量
     */
    public void restoreEnergy(float amount) {
        float newEnergy = Math.min(1, currentState.getEnergyLevel() + amount);
        this.currentState = currentState.withEnergyLevel(newEnergy);
        logger.debug("Energy restored to: {:.0f}%", newEnergy * 100);
    }

    /**
     * 更新一致性分数
     */
    public void updateCoherence(float delta) {
        float newCoherence = Math.max(0, Math.min(1, currentState.getCoherenceScore() + delta));
        this.currentState = currentState.withCoherenceScore(newCoherence);
        logger.debug("Coherence updated to: {:.0f}%", newCoherence * 100);
    }

    /**
     * 完整性检查
     */
    public boolean isComplete() {
        return currentState != null &&
                assessment != null &&
                currentFocus != null &&
                boundaries != null;
    }
}
