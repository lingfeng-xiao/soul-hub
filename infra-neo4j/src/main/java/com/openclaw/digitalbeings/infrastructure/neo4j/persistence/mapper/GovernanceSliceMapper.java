package com.openclaw.digitalbeings.infrastructure.neo4j.persistence.mapper;

import com.openclaw.digitalbeings.domain.governance.ManagedAgentSpec;
import com.openclaw.digitalbeings.domain.governance.OwnerProfileFact;
import com.openclaw.digitalbeings.infrastructure.neo4j.persistence.entity.ManagedAgentSpecNode;
import com.openclaw.digitalbeings.infrastructure.neo4j.persistence.entity.OwnerProfileFactNode;
import java.util.List;

final class GovernanceSliceMapper {

    private GovernanceSliceMapper() {
    }

    static List<OwnerProfileFactNode> toOwnerProfileFactNodes(List<OwnerProfileFact> ownerProfileFacts) {
        return ownerProfileFacts.stream()
                .map(fact -> new OwnerProfileFactNode(
                        fact.factId(),
                        fact.section(),
                        fact.key(),
                        fact.summary(),
                        fact.acceptedAt()
                ))
                .toList();
    }

    static List<OwnerProfileFact> toOwnerProfileFacts(List<OwnerProfileFactNode> nodes) {
        return nodes.stream()
                .map(node -> new OwnerProfileFact(
                        node.getFactId(),
                        node.getSection(),
                        node.getKey(),
                        node.getSummary(),
                        node.getAcceptedAt()
                ))
                .toList();
    }

    static List<ManagedAgentSpecNode> toManagedAgentSpecNodes(List<ManagedAgentSpec> managedAgentSpecs) {
        return managedAgentSpecs.stream()
                .map(spec -> new ManagedAgentSpecNode(
                        spec.managedAgentId(),
                        spec.role(),
                        spec.status(),
                        spec.createdAt()
                ))
                .toList();
    }

    static List<ManagedAgentSpec> toManagedAgentSpecs(List<ManagedAgentSpecNode> nodes) {
        return nodes.stream()
                .map(node -> new ManagedAgentSpec(
                        node.getManagedAgentId(),
                        node.getRole(),
                        node.getStatus(),
                        node.getCreatedAt()
                ))
                .toList();
    }
}
