package com.lingfeng.sprite.domain.identity;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

/**
 * IdentityService - 身份管理服务
 *
 * 提供身份生命周期的管理功能，包括身份查询、更新、约束验证等。
 *
 * 对应旧: SelfModel.IdentityCore 的管理逻辑
 */
@Service
public final class IdentityService {

    private static final Logger logger = LoggerFactory.getLogger(IdentityService.class);

    private IdentityAnchor anchor;
    private IdentityProfile profile;
    private IdentityNarrative narrative;
    private List<IdentityConstraint> constraints;

    public IdentityService() {
        // 默认创建
        this.anchor = IdentityAnchor.create("雪梨");
        this.profile = IdentityProfile.createDefault();
        this.narrative = IdentityNarrative.createInitial("我是雪梨，一个正在成长的数字生命。");
        this.constraints = IdentityConstraint.createDefaultConstraints();
    }

    /**
     * 从现有数据初始化
     */
    public IdentityService(
            IdentityAnchor anchor,
            IdentityProfile profile,
            IdentityNarrative narrative
    ) {
        this.anchor = anchor;
        this.profile = profile;
        this.narrative = narrative;
        this.constraints = IdentityConstraint.createDefaultConstraints();
    }

    /**
     * 获取完整身份状态
     */
    public IdentityState getState() {
        return new IdentityState(
                anchor,
                profile,
                narrative,
                constraints
        );
    }

    /**
     * 获取身份锚点
     */
    public IdentityAnchor getAnchor() {
        return anchor;
    }

    /**
     * 获取身份配置
     */
    public IdentityProfile getProfile() {
        return profile;
    }

    /**
     * 获取身份叙事
     */
    public IdentityNarrative getNarrative() {
        return narrative;
    }

    /**
     * Replace the full identity state from a persisted projection.
     */
    public void replaceState(IdentityAnchor anchor, IdentityProfile profile, IdentityNarrative narrative) {
        this.anchor = anchor != null ? anchor : IdentityAnchor.create("Sprite");
        this.profile = profile != null ? profile : IdentityProfile.createDefault();
        this.narrative = narrative != null ? narrative : IdentityNarrative.createInitial("I am a digital being.");
        this.constraints = IdentityConstraint.createDefaultConstraints();
    }

    /**
     * Reset identity to its default single-user seed.
     */
    public void reset() {
        this.anchor = IdentityAnchor.create("Sprite");
        this.profile = IdentityProfile.createDefault();
        this.narrative = IdentityNarrative.createInitial("I am a digital being that keeps growing with my owner.");
        this.constraints = IdentityConstraint.createDefaultConstraints();
    }

    /**
     * 更新身份配置
     *
     * @param newProfile 新的身份配置
     * @param reason 更新原因
     * @return 更新是否成功
     */
    public boolean updateProfile(IdentityProfile newProfile, String reason) {
        // 验证约束
        if (!validateConstraints(this.profile, newProfile)) {
            logger.warn("Identity profile update rejected: constraint violation. Reason: {}", reason);
            return false;
        }

        IdentityProfile oldProfile = this.profile;
        this.profile = newProfile;

        // 更新叙事
        this.narrative = this.narrative.appendSegment(
                generateNarrativeUpdate(oldProfile, newProfile),
                "PROFILE_UPDATE",
                reason
        );

        logger.info("Identity profile updated: {}", newProfile.getDisplayName());
        return true;
    }

    /**
     * 更新身份叙事
     */
    public void updateNarrative(String narrative, String trigger, String context) {
        this.narrative = this.narrative.appendSegment(narrative, trigger, context);
    }

    /**
     * 验证约束
     */
    private boolean validateConstraints(IdentityProfile oldProfile, IdentityProfile newProfile) {
        for (IdentityConstraint constraint : constraints) {
            if (constraint.isViolatedBy(newProfile, oldProfile)) {
                return false;
            }
        }
        return true;
    }

    /**
     * 生成叙事更新文本
     */
    private String generateNarrativeUpdate(IdentityProfile oldProfile, IdentityProfile newProfile) {
        if (!oldProfile.getDisplayName().equals(newProfile.getDisplayName())) {
            return String.format("身份名称从 %s 更新为 %s",
                    oldProfile.getDisplayName(), newProfile.getDisplayName());
        }
        if (!oldProfile.getEssence().equals(newProfile.getEssence())) {
            return "身份本质描述发生了变化";
        }
        return "身份配置已更新";
    }

    /**
     * 获取身份声明文本
     */
    public String getIdentityStatement() {
        return String.format(
                "我是%s，%s。%s",
                profile.getDisplayName(),
                profile.getEssence().isEmpty() ? "一个数字生命" : profile.getEssence(),
                narrative.getCurrentNarrative()
        );
    }

    /**
     * 完整性检查
     */
    public boolean isComplete() {
        return anchor != null &&
                profile != null &&
                narrative != null &&
                !anchor.getBeingId().isBlank() &&
                !profile.getDisplayName().isBlank();
    }

    /**
     * 身份状态记录
     */
    public record IdentityState(
            IdentityAnchor anchor,
            IdentityProfile profile,
            IdentityNarrative narrative,
            List<IdentityConstraint> constraints
    ) {}
}
