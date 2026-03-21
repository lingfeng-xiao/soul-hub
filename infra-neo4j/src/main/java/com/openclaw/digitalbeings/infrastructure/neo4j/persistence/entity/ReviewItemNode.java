package com.openclaw.digitalbeings.infrastructure.neo4j.persistence.entity;

import com.openclaw.digitalbeings.domain.core.ReviewItemStatus;
import java.time.Instant;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Property;

@Node("ReviewItem")
public class ReviewItemNode {

    @Id
    private String reviewItemId;

    @Property("lane")
    private String lane;

    @Property("kind")
    private String kind;

    @Property("proposal")
    private String proposal;

    @Property("status")
    private ReviewItemStatus status;

    @Property("createdAt")
    private Instant createdAt;

    @Property("updatedAt")
    private Instant updatedAt;

    @Property("lastActor")
    private String lastActor;

    public ReviewItemNode() {
    }

    public ReviewItemNode(
            String reviewItemId,
            String lane,
            String kind,
            String proposal,
            ReviewItemStatus status,
            Instant createdAt,
            Instant updatedAt,
            String lastActor
    ) {
        this.reviewItemId = reviewItemId;
        this.lane = lane;
        this.kind = kind;
        this.proposal = proposal;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.lastActor = lastActor;
    }

    public String getReviewItemId() {
        return reviewItemId;
    }

    public void setReviewItemId(String reviewItemId) {
        this.reviewItemId = reviewItemId;
    }

    public String getLane() {
        return lane;
    }

    public void setLane(String lane) {
        this.lane = lane;
    }

    public String getKind() {
        return kind;
    }

    public void setKind(String kind) {
        this.kind = kind;
    }

    public String getProposal() {
        return proposal;
    }

    public void setProposal(String proposal) {
        this.proposal = proposal;
    }

    public ReviewItemStatus getStatus() {
        return status;
    }

    public void setStatus(ReviewItemStatus status) {
        this.status = status;
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

    public String getLastActor() {
        return lastActor;
    }

    public void setLastActor(String lastActor) {
        this.lastActor = lastActor;
    }
}
