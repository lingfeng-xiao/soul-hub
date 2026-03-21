package com.openclaw.digitalbeings.infrastructure.neo4j.persistence.entity;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Property;

@Node("CanonicalProjection")
public class CanonicalProjectionNode {

    @Id
    private String projectionId;

    @Property("version")
    private long version;

    @Property("generatedAt")
    private Instant generatedAt;

    @Property("acceptedReviewItemIds")
    private List<String> acceptedReviewItemIds = new ArrayList<>();

    @Property("contentSummary")
    private String contentSummary;

    public CanonicalProjectionNode() {
    }

    public CanonicalProjectionNode(
            String projectionId,
            long version,
            Instant generatedAt,
            List<String> acceptedReviewItemIds,
            String contentSummary
    ) {
        this.projectionId = projectionId;
        this.version = version;
        this.generatedAt = generatedAt;
        this.acceptedReviewItemIds = new ArrayList<>(acceptedReviewItemIds);
        this.contentSummary = contentSummary;
    }

    public String getProjectionId() {
        return projectionId;
    }

    public void setProjectionId(String projectionId) {
        this.projectionId = projectionId;
    }

    public long getVersion() {
        return version;
    }

    public void setVersion(long version) {
        this.version = version;
    }

    public Instant getGeneratedAt() {
        return generatedAt;
    }

    public void setGeneratedAt(Instant generatedAt) {
        this.generatedAt = generatedAt;
    }

    public List<String> getAcceptedReviewItemIds() {
        return acceptedReviewItemIds;
    }

    public void setAcceptedReviewItemIds(List<String> acceptedReviewItemIds) {
        this.acceptedReviewItemIds = new ArrayList<>(acceptedReviewItemIds);
    }

    public String getContentSummary() {
        return contentSummary;
    }

    public void setContentSummary(String contentSummary) {
        this.contentSummary = contentSummary;
    }
}
