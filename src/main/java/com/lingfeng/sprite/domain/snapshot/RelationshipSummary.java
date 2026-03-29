package com.lingfeng.sprite.domain.snapshot;

/**
 * RelationshipSummary - 关系摘要
 *
 * 用于 LifeSnapshot 中的关系状态摘要。
 */
public final class RelationshipSummary {

    private final String relationshipType;
    private final String trustLevel;
    private final float trustScore;
    private final float relationshipStrength;
    private final int interactionCount;
    private final int sharedProjectsCount;
    private final String topCarePriority;

    private RelationshipSummary(Builder builder) {
        this.relationshipType = builder.relationshipType;
        this.trustLevel = builder.trustLevel;
        this.trustScore = builder.trustScore;
        this.relationshipStrength = builder.relationshipStrength;
        this.interactionCount = builder.interactionCount;
        this.sharedProjectsCount = builder.sharedProjectsCount;
        this.topCarePriority = builder.topCarePriority;
    }

    public static RelationshipSummary createDefault() {
        return new Builder()
                .relationshipType("FRIEND")
                .trustLevel("MEDIUM")
                .trustScore(0.5f)
                .relationshipStrength(0.5f)
                .interactionCount(0)
                .sharedProjectsCount(0)
                .topCarePriority("EMOTIONAL")
                .build();
    }

    // Getters
    public String getRelationshipType() { return relationshipType; }
    public String getTrustLevel() { return trustLevel; }
    public float getTrustScore() { return trustScore; }
    public float getRelationshipStrength() { return relationshipStrength; }
    public int getInteractionCount() { return interactionCount; }
    public int getSharedProjectsCount() { return sharedProjectsCount; }
    public String getTopCarePriority() { return topCarePriority; }

    public static Builder builder() { return new Builder(); }

    public static final class Builder {
        private String relationshipType = "FRIEND";
        private String trustLevel = "MEDIUM";
        private float trustScore = 0.5f;
        private float relationshipStrength = 0.5f;
        private int interactionCount = 0;
        private int sharedProjectsCount = 0;
        private String topCarePriority = "EMOTIONAL";

        public Builder relationshipType(String relationshipType) { this.relationshipType = relationshipType; return this; }
        public Builder trustLevel(String trustLevel) { this.trustLevel = trustLevel; return this; }
        public Builder trustScore(float trustScore) { this.trustScore = trustScore; return this; }
        public Builder relationshipStrength(float relationshipStrength) { this.relationshipStrength = relationshipStrength; return this; }
        public Builder interactionCount(int interactionCount) { this.interactionCount = interactionCount; return this; }
        public Builder sharedProjectsCount(int sharedProjectsCount) { this.sharedProjectsCount = sharedProjectsCount; return this; }
        public Builder topCarePriority(String topCarePriority) { this.topCarePriority = topCarePriority; return this; }

        public RelationshipSummary build() { return new RelationshipSummary(this); }
    }
}
