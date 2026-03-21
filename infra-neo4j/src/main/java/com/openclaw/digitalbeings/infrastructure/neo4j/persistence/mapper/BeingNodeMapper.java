package com.openclaw.digitalbeings.infrastructure.neo4j.persistence.mapper;

import com.openclaw.digitalbeings.domain.being.Being;
import com.openclaw.digitalbeings.domain.runtime.AuthorityLease;
import com.openclaw.digitalbeings.infrastructure.neo4j.persistence.entity.AuthorityLeaseNode;
import com.openclaw.digitalbeings.infrastructure.neo4j.persistence.entity.BeingNode;
import java.util.List;

public final class BeingNodeMapper {

    private BeingNodeMapper() {
    }

    public static BeingNode fromDomain(Being being) {
        BeingNode node = new BeingNode();
        node.setBeingId(being.beingId().value());
        node.setDisplayName(being.displayName());
        node.setCreatedAt(being.createdAt());
        node.setRevision(being.revision());
        node.setIdentityFacets(IdentitySliceMapper.toIdentityFacetNodes(being.identityFacets()));
        node.setRelationships(IdentitySliceMapper.toRelationshipNodes(being.relationships()));
        node.setHostContracts(RuntimeSliceMapper.toHostContractNodes(being.hostContracts()));
        node.setRuntimeSessions(RuntimeSliceMapper.toSessionNodes(being.runtimeSessions()));
        node.setAuthorityLeases(RuntimeSliceMapper.toLeaseNodes(being.authorityLeases()));
        node.setReviewItems(ReviewSliceMapper.toReviewNodes(being.reviewItems()));
        node.setCanonicalProjection(ReviewSliceMapper.toCanonicalProjectionNode(being.canonicalProjection().orElse(null)));
        node.setOwnerProfileFacts(GovernanceSliceMapper.toOwnerProfileFactNodes(being.ownerProfileFacts()));
        node.setManagedAgentSpecs(GovernanceSliceMapper.toManagedAgentSpecNodes(being.managedAgentSpecs()));
        node.setContinuitySnapshots(SnapshotSliceMapper.toSnapshotNodes(being.continuitySnapshots()));
        node.setDomainEvents(AuditSliceMapper.toEventNodes(being.domainEvents()));
        return node;
    }

    public static Being toDomain(BeingNode node) {
        Being being = BeingReflectionSupport.instantiateBeing(node.getBeingId(), node.getDisplayName(), node.getCreatedAt());
        BeingReflectionSupport.setField(being, "revision", node.getRevision());
        BeingReflectionSupport.setField(being, "identityFacets", IdentitySliceMapper.toIdentityFacets(node.getIdentityFacets()));
        BeingReflectionSupport.setField(being, "relationships", IdentitySliceMapper.toRelationships(node.getRelationships()));
        BeingReflectionSupport.setField(being, "hostContracts", RuntimeSliceMapper.toHostContracts(node.getHostContracts()));
        BeingReflectionSupport.setField(being, "runtimeSessions", RuntimeSliceMapper.toRuntimeSessions(node.getRuntimeSessions()));
        BeingReflectionSupport.setField(being, "authorityLeases", RuntimeSliceMapper.toAuthorityLeases(node.getAuthorityLeases()));
        BeingReflectionSupport.setField(being, "reviewItems", ReviewSliceMapper.toReviewItems(node.getReviewItems()));
        BeingReflectionSupport.setField(being, "canonicalProjection", ReviewSliceMapper.toCanonicalProjection(node.getCanonicalProjection()));
        BeingReflectionSupport.setField(being, "ownerProfileFacts", GovernanceSliceMapper.toOwnerProfileFacts(node.getOwnerProfileFacts()));
        BeingReflectionSupport.setField(being, "managedAgentSpecs", GovernanceSliceMapper.toManagedAgentSpecs(node.getManagedAgentSpecs()));
        BeingReflectionSupport.setField(being, "continuitySnapshots", SnapshotSliceMapper.toSnapshots(node.getContinuitySnapshots()));
        BeingReflectionSupport.setField(being, "domainEvents", AuditSliceMapper.toDomainEvents(node.getDomainEvents()));
        return being;
    }

    public static AuthorityLeaseNode toLeaseNode(AuthorityLease lease) {
        return RuntimeSliceMapper.toLeaseNodes(List.of(lease)).getFirst();
    }

    public static List<AuthorityLeaseNode> toLeaseNodes(List<AuthorityLease> leases) {
        return RuntimeSliceMapper.toLeaseNodes(leases);
    }
}
