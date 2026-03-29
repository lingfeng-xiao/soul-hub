package com.lingfeng.sprite.domain.relationship;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * RelationshipService - 关系领域服务
 *
 * 提供关系状态的统一管理，包括信任管理、关怀优先级、共同项目等。
 *
 * 对应旧: OwnerModel 和 WorldModel.SocialGraph 的关系管理逻辑
 */
@Service
public final class RelationshipService {

    private static final Logger logger = LoggerFactory.getLogger(RelationshipService.class);

    private String ownerId;
    private RelationshipProfile profile;
    private TrustState trustState;
    private Map<String, SharedProjectLink> sharedProjects;
    private List<CarePriority> carePriorities;

    public RelationshipService() {
        this.sharedProjects = new ConcurrentHashMap<>();
        this.carePriorities = new ArrayList<>();
    }

    /**
     * 初始化关系
     */
    public void initialize(String ownerId) {
        this.ownerId = ownerId;
        this.profile = RelationshipProfile.createDefault(ownerId);
        this.trustState = TrustState.createDefault();
        initializeDefaultCarePriorities();
        logger.info("Relationship initialized for owner: {}", ownerId);
    }

    private void initializeDefaultCarePriorities() {
        this.carePriorities = new ArrayList<>();
        this.carePriorities.add(CarePriority.createSafety());
        this.carePriorities.add(CarePriority.createEmotional());
        this.carePriorities.add(CarePriority.createProductivity());
        this.carePriorities.add(CarePriority.createGrowth());
    }

    // ==================== Getters ====================

    public String getOwnerId() {
        return ownerId;
    }

    public RelationshipProfile getProfile() {
        return profile;
    }

    public TrustState getTrustState() {
        return trustState;
    }

    public List<SharedProjectLink> getSharedProjects() {
        return new ArrayList<>(sharedProjects.values());
    }

    public List<CarePriority> getCarePriorities() {
        return new ArrayList<>(carePriorities);
    }

    /**
     * Replace the full relationship state from persistence.
     */
    public void replaceState(
            String ownerId,
            RelationshipProfile profile,
            TrustState trustState,
            List<SharedProjectLink> sharedProjects,
            List<CarePriority> carePriorities
    ) {
        this.ownerId = ownerId;
        this.profile = profile != null ? profile : RelationshipProfile.createDefault(ownerId);
        this.trustState = trustState != null ? trustState : TrustState.createDefault();
        this.sharedProjects = new ConcurrentHashMap<>();
        if (sharedProjects != null) {
            sharedProjects.forEach(project -> this.sharedProjects.put(project.getProjectId(), project));
        }
        this.carePriorities = carePriorities != null && !carePriorities.isEmpty()
                ? new ArrayList<>(carePriorities)
                : new ArrayList<>();
        if (this.carePriorities.isEmpty()) {
            initializeDefaultCarePriorities();
        }
    }

    /**
     * Reset relationship state to defaults for the given owner.
     */
    public void reset(String ownerId) {
        initialize(ownerId);
    }

    // ==================== Relationship Operations ====================

    /**
     * 记录一次交互
     */
    public void recordInteraction() {
        this.profile = profile.recordInteraction();
        logger.debug("Interaction recorded: count={}", profile.getInteractionCount());
    }

    /**
     * 更新关系强度
     */
    public void updateStrength(float newStrength) {
        this.profile = profile.withStrength(newStrength);
        logger.debug("Relationship strength updated: {}", newStrength);
    }

    /**
     * 更新关系类型
     */
    public void updateRelationshipType(RelationshipProfile.RelationshipType type) {
        this.profile = this.profile.with()
                .type(type)
                .build();
        logger.debug("Relationship type updated: {}", type);
    }

    // ==================== Trust Operations ====================

    /**
     * 增加信任
     */
    public void increaseTrust(float amount) {
        TrustState oldState = this.trustState;
        this.trustState = trustState.increaseTrust(amount);
        logger.info("Trust increased: {} -> {} (amount={})",
                oldState.getScore(), this.trustState.getScore(), amount);
    }

    /**
     * 减少信任
     */
    public void decreaseTrust(float amount) {
        TrustState oldState = this.trustState;
        this.trustState = trustState.decreaseTrust(amount);
        logger.warn("Trust decreased: {} -> {} (amount={})",
                oldState.getScore(), this.trustState.getScore(), amount);
    }

    /**
     * 检查信任等级
     */
    public boolean isTrustAtLeast(TrustState.TrustLevel minimumLevel) {
        return trustState.getLevel().ordinal() >= minimumLevel.ordinal();
    }

    // ==================== Shared Project Operations ====================

    /**
     * 添加共同项目
     */
    public void addSharedProject(String projectId, String name, String description) {
        SharedProjectLink project = SharedProjectLink.create(projectId, name, description);
        this.sharedProjects.put(projectId, project);
        logger.info("Shared project added: {} - {}", projectId, name);
    }

    /**
     * 更新项目参与度
     */
    public void updateProjectEngagement(String projectId, float engagement) {
        SharedProjectLink project = sharedProjects.get(projectId);
        if (project != null) {
            this.sharedProjects.put(projectId, project.withEngagement(engagement));
            logger.debug("Project engagement updated: {} -> {}", projectId, engagement);
        }
    }

    /**
     * 标记项目完成
     */
    public void completeProject(String projectId) {
        SharedProjectLink project = sharedProjects.get(projectId);
        if (project != null) {
            this.sharedProjects.put(projectId, project.markCompleted());
            logger.info("Project completed: {}", projectId);
        }
    }

    /**
     * 获取活跃项目
     */
    public List<SharedProjectLink> getActiveProjects() {
        return sharedProjects.values().stream()
                .filter(p -> p.getStatus() == SharedProjectLink.ProjectStatus.ACTIVE)
                .toList();
    }

    // ==================== Care Priority Operations ====================

    /**
     * 获取最高优先级的关怀
     */
    public CarePriority getTopCarePriority() {
        return carePriorities.stream()
                .filter(CarePriority::isEnabled)
                .max((a, b) -> Float.compare(a.getScore(), b.getScore()))
                .orElse(null);
    }

    /**
     * 更新关怀优先级
     */
    public void updateCarePriority(CarePriority.CareType careType, CarePriority.PriorityLevel level) {
        for (int i = 0; i < carePriorities.size(); i++) {
            if (carePriorities.get(i).getCareType() == careType) {
                carePriorities.set(i, carePriorities.get(i).withLevel(level));
                logger.debug("Care priority updated: {} -> {}", careType, level);
                return;
            }
        }
    }

    /**
     * 触发关怀
     */
    public void triggerCare(CarePriority.CareType careType) {
        for (int i = 0; i < carePriorities.size(); i++) {
            if (carePriorities.get(i).getCareType() == careType) {
                carePriorities.set(i, carePriorities.get(i).trigger());
                logger.info("Care triggered: {}", careType);
                return;
            }
        }
    }

    // ==================== Summary ====================

    /**
     * 获取关系摘要
     */
    public String getRelationshipSummary() {
        return String.format(
                "关系: %s | 信任: %s (%.0f%%) | 项目: %d个活跃 | 关怀: %s",
                profile.getType(),
                trustState.getLevel(),
                trustState.getScore() * 100,
                getActiveProjects().size(),
                getTopCarePriority() != null ? getTopCarePriority().getCareType() : "无"
        );
    }

    /**
     * 完整性检查
     */
    public boolean isComplete() {
        return ownerId != null &&
                profile != null &&
                trustState != null &&
                !carePriorities.isEmpty();
    }

    // ==================== Builder ====================

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private String ownerId;
        private RelationshipProfile profile;
        private TrustState trustState;
        private List<SharedProjectLink> sharedProjects = new ArrayList<>();
        private List<CarePriority> carePriorities = new ArrayList<>();

        public Builder ownerId(String ownerId) {
            this.ownerId = ownerId;
            return this;
        }

        public Builder profile(RelationshipProfile profile) {
            this.profile = profile;
            return this;
        }

        public Builder trustState(TrustState trustState) {
            this.trustState = trustState;
            return this;
        }

        public Builder sharedProjects(List<SharedProjectLink> sharedProjects) {
            this.sharedProjects = sharedProjects != null ? sharedProjects : new ArrayList<>();
            return this;
        }

        public Builder carePriorities(List<CarePriority> carePriorities) {
            this.carePriorities = carePriorities != null ? carePriorities : new ArrayList<>();
            return this;
        }

        public RelationshipService build() {
            RelationshipService service = new RelationshipService();
            if (this.ownerId != null) {
                service.initialize(this.ownerId);
            }
            if (this.profile != null) {
                service.profile = this.profile;
            }
            if (this.trustState != null) {
                service.trustState = this.trustState;
            }
            if (!this.sharedProjects.isEmpty()) {
                this.sharedProjects.forEach(p -> service.sharedProjects.put(p.getProjectId(), p));
            }
            if (!this.carePriorities.isEmpty()) {
                service.carePriorities = new ArrayList<>(this.carePriorities);
            }
            return service;
        }
    }
}
