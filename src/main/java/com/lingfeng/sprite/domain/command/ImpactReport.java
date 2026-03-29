package com.lingfeng.sprite.domain.command;

/**
 * ImpactReport - 影响报告
 *
 * 描述命令执行后对各生命核心的影响。
 * 这是命令结果生命回流机制的核心数据结构。
 *
 * 对应 IGN-023
 */
public final class ImpactReport {

    private final SelfUpdate selfUpdate;
    private final RelationshipUpdate relationshipUpdate;
    private final GoalUpdate goalUpdate;
    private final MemoryUpdate memoryUpdate;
    private final GrowthUpdate growthUpdate;

    private ImpactReport(Builder builder) {
        this.selfUpdate = builder.selfUpdate;
        this.relationshipUpdate = builder.relationshipUpdate;
        this.goalUpdate = builder.goalUpdate;
        this.memoryUpdate = builder.memoryUpdate;
        this.growthUpdate = builder.growthUpdate;
    }

    public static ImpactReport none() {
        return new Builder().build();
    }

    public static Builder builder() { return new Builder(); }

    public SelfUpdate getSelfUpdate() { return selfUpdate; }
    public RelationshipUpdate getRelationshipUpdate() { return relationshipUpdate; }
    public GoalUpdate getGoalUpdate() { return goalUpdate; }
    public MemoryUpdate getMemoryUpdate() { return memoryUpdate; }
    public GrowthUpdate getGrowthUpdate() { return growthUpdate; }

    public boolean hasAnyImpact() {
        return (selfUpdate != null && selfUpdate.hasImpact()) ||
               (relationshipUpdate != null && relationshipUpdate.hasImpact()) ||
               (goalUpdate != null && goalUpdate.hasImpact()) ||
               (memoryUpdate != null && memoryUpdate.hasImpact()) ||
               (growthUpdate != null && growthUpdate.hasImpact());
    }

    public static final class Builder {
        private SelfUpdate selfUpdate = SelfUpdate.NONE;
        private RelationshipUpdate relationshipUpdate = RelationshipUpdate.NONE;
        private GoalUpdate goalUpdate = GoalUpdate.NONE;
        private MemoryUpdate memoryUpdate = MemoryUpdate.NONE;
        private GrowthUpdate growthUpdate = GrowthUpdate.NONE;

        public Builder selfUpdate(SelfUpdate selfUpdate) { this.selfUpdate = selfUpdate; return this; }
        public Builder relationshipUpdate(RelationshipUpdate relationshipUpdate) { this.relationshipUpdate = relationshipUpdate; return this; }
        public Builder goalUpdate(GoalUpdate goalUpdate) { this.goalUpdate = goalUpdate; return this; }
        public Builder memoryUpdate(MemoryUpdate memoryUpdate) { this.memoryUpdate = memoryUpdate; return this; }
        public Builder growthUpdate(GrowthUpdate growthUpdate) { this.growthUpdate = growthUpdate; return this; }

        public ImpactReport build() { return new ImpactReport(this); }
    }

    /**
     * 自我更新
     */
    public record SelfUpdate(
            boolean energyChanged,
            float energyDelta,
            boolean focusChanged,
            String newFocus,
            String observation
    ) {
        public static final SelfUpdate NONE = new SelfUpdate(false, 0, false, "", "");

        public boolean hasImpact() { return energyChanged || focusChanged; }
    }

    /**
     * 关系更新
     */
    public record RelationshipUpdate(
            boolean interacted,
            boolean trustChanged,
            float trustDelta,
            String interactionType
    ) {
        public static final RelationshipUpdate NONE = new RelationshipUpdate(false, false, 0, "");

        public boolean hasImpact() { return interacted || trustChanged; }
    }

    /**
     * 目标更新
     */
    public record GoalUpdate(
            boolean intentionTriggered,
            String intentionId,
            boolean goalProgressed,
            String goalId,
            float progressDelta
    ) {
        public static final GoalUpdate NONE = new GoalUpdate(false, "", false, "", 0);

        public boolean hasImpact() { return intentionTriggered || goalProgressed; }
    }

    /**
     * 记忆更新
     */
    public record MemoryUpdate(
            boolean memoryCreated,
            String memoryType,
            String memoryId
    ) {
        public static final MemoryUpdate NONE = new MemoryUpdate(false, "", "");

        public boolean hasImpact() { return memoryCreated; }
    }

    /**
     * 成长更新
     */
    public record GrowthUpdate(
            boolean patternLearned,
            String patternType,
            String insight,
            boolean skillAcquired,
            String skillName
    ) {
        public static final GrowthUpdate NONE = new GrowthUpdate(false, "", "", false, "");

        public boolean hasImpact() { return patternLearned || skillAcquired; }
    }
}
