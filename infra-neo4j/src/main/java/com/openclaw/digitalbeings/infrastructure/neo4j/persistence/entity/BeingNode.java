package com.openclaw.digitalbeings.infrastructure.neo4j.persistence.entity;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Property;
import org.springframework.data.neo4j.core.schema.Relationship;

@Node("Being")
public class BeingNode {

    @Id
    private String beingId;

    @Property("displayName")
    private String displayName;

    @Property("createdAt")
    private Instant createdAt;

    @Property("revision")
    private long revision;

    @Relationship(type = "HAS_IDENTITY")
    private List<IdentityFacetNode> identityFacets = new ArrayList<>();

    @Relationship(type = "RELATES_TO")
    private List<RelationshipEntityNode> relationships = new ArrayList<>();

    @Relationship(type = "ALLOWED_ON")
    private List<HostContractNode> hostContracts = new ArrayList<>();

    @Relationship(type = "HOLDS_LEASE")
    private List<AuthorityLeaseNode> authorityLeases = new ArrayList<>();

    @Relationship(type = "HAS_SESSION")
    private List<RuntimeSessionNode> runtimeSessions = new ArrayList<>();

    @Relationship(type = "HAS_REVIEW_ITEM")
    private List<ReviewItemNode> reviewItems = new ArrayList<>();

    @Relationship(type = "AFFECTS")
    private CanonicalProjectionNode canonicalProjection;

    @Relationship(type = "HAS_PROFILE_FACT")
    private List<OwnerProfileFactNode> ownerProfileFacts = new ArrayList<>();

    @Relationship(type = "GOVERNS")
    private List<ManagedAgentSpecNode> managedAgentSpecs = new ArrayList<>();

    @Relationship(type = "HAS_SNAPSHOT")
    private List<ContinuitySnapshotNode> continuitySnapshots = new ArrayList<>();

    @Relationship(type = "EMITTED_EVENT")
    private List<DomainEventNode> domainEvents = new ArrayList<>();

    public BeingNode() {
    }

    public String getBeingId() {
        return beingId;
    }

    public void setBeingId(String beingId) {
        this.beingId = beingId;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public long getRevision() {
        return revision;
    }

    public void setRevision(long revision) {
        this.revision = revision;
    }

    public List<IdentityFacetNode> getIdentityFacets() {
        return identityFacets;
    }

    public void setIdentityFacets(List<IdentityFacetNode> identityFacets) {
        this.identityFacets = new ArrayList<>(identityFacets);
    }

    public List<RelationshipEntityNode> getRelationships() {
        return relationships;
    }

    public void setRelationships(List<RelationshipEntityNode> relationships) {
        this.relationships = new ArrayList<>(relationships);
    }

    public List<HostContractNode> getHostContracts() {
        return hostContracts;
    }

    public void setHostContracts(List<HostContractNode> hostContracts) {
        this.hostContracts = new ArrayList<>(hostContracts);
    }

    public List<AuthorityLeaseNode> getAuthorityLeases() {
        return authorityLeases;
    }

    public void setAuthorityLeases(List<AuthorityLeaseNode> authorityLeases) {
        this.authorityLeases = new ArrayList<>(authorityLeases);
    }

    public List<RuntimeSessionNode> getRuntimeSessions() {
        return runtimeSessions;
    }

    public void setRuntimeSessions(List<RuntimeSessionNode> runtimeSessions) {
        this.runtimeSessions = new ArrayList<>(runtimeSessions);
    }

    public List<ReviewItemNode> getReviewItems() {
        return reviewItems;
    }

    public void setReviewItems(List<ReviewItemNode> reviewItems) {
        this.reviewItems = new ArrayList<>(reviewItems);
    }

    public CanonicalProjectionNode getCanonicalProjection() {
        return canonicalProjection;
    }

    public void setCanonicalProjection(CanonicalProjectionNode canonicalProjection) {
        this.canonicalProjection = canonicalProjection;
    }

    public List<OwnerProfileFactNode> getOwnerProfileFacts() {
        return ownerProfileFacts;
    }

    public void setOwnerProfileFacts(List<OwnerProfileFactNode> ownerProfileFacts) {
        this.ownerProfileFacts = new ArrayList<>(ownerProfileFacts);
    }

    public List<ManagedAgentSpecNode> getManagedAgentSpecs() {
        return managedAgentSpecs;
    }

    public void setManagedAgentSpecs(List<ManagedAgentSpecNode> managedAgentSpecs) {
        this.managedAgentSpecs = new ArrayList<>(managedAgentSpecs);
    }

    public List<ContinuitySnapshotNode> getContinuitySnapshots() {
        return continuitySnapshots;
    }

    public void setContinuitySnapshots(List<ContinuitySnapshotNode> continuitySnapshots) {
        this.continuitySnapshots = new ArrayList<>(continuitySnapshots);
    }

    public List<DomainEventNode> getDomainEvents() {
        return domainEvents;
    }

    public void setDomainEvents(List<DomainEventNode> domainEvents) {
        this.domainEvents = new ArrayList<>(domainEvents);
    }
}
