package com.openclaw.digitalbeings.infrastructure.neo4j.persistence.entity;

import com.openclaw.digitalbeings.domain.core.AuthorityLeaseStatus;
import java.time.Instant;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Property;

@Node("AuthorityLease")
public class AuthorityLeaseNode {

    @Id
    private String leaseId;

    @Property("sessionId")
    private String sessionId;

    @Property("status")
    private AuthorityLeaseStatus status;

    @Property("requestedAt")
    private Instant requestedAt;

    @Property("grantedAt")
    private Instant grantedAt;

    @Property("releasedAt")
    private Instant releasedAt;

    @Property("lastActor")
    private String lastActor;

    public AuthorityLeaseNode() {
    }

    public AuthorityLeaseNode(
            String leaseId,
            String sessionId,
            AuthorityLeaseStatus status,
            Instant requestedAt,
            Instant grantedAt,
            Instant releasedAt,
            String lastActor
    ) {
        this.leaseId = leaseId;
        this.sessionId = sessionId;
        this.status = status;
        this.requestedAt = requestedAt;
        this.grantedAt = grantedAt;
        this.releasedAt = releasedAt;
        this.lastActor = lastActor;
    }

    public String getLeaseId() {
        return leaseId;
    }

    public void setLeaseId(String leaseId) {
        this.leaseId = leaseId;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public AuthorityLeaseStatus getStatus() {
        return status;
    }

    public void setStatus(AuthorityLeaseStatus status) {
        this.status = status;
    }

    public Instant getRequestedAt() {
        return requestedAt;
    }

    public void setRequestedAt(Instant requestedAt) {
        this.requestedAt = requestedAt;
    }

    public Instant getGrantedAt() {
        return grantedAt;
    }

    public void setGrantedAt(Instant grantedAt) {
        this.grantedAt = grantedAt;
    }

    public Instant getReleasedAt() {
        return releasedAt;
    }

    public void setReleasedAt(Instant releasedAt) {
        this.releasedAt = releasedAt;
    }

    public String getLastActor() {
        return lastActor;
    }

    public void setLastActor(String lastActor) {
        this.lastActor = lastActor;
    }
}
