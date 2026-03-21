package com.openclaw.digitalbeings.infrastructure.neo4j.persistence.mapper;

import com.openclaw.digitalbeings.domain.identity.IdentityFacet;
import com.openclaw.digitalbeings.domain.identity.RelationshipEntity;
import com.openclaw.digitalbeings.infrastructure.neo4j.persistence.entity.IdentityFacetNode;
import com.openclaw.digitalbeings.infrastructure.neo4j.persistence.entity.RelationshipEntityNode;
import java.util.List;

final class IdentitySliceMapper {

    private IdentitySliceMapper() {
    }

    static List<IdentityFacetNode> toIdentityFacetNodes(List<IdentityFacet> facets) {
        return facets.stream()
                .map(facet -> new IdentityFacetNode(
                        facet.facetId(),
                        facet.kind(),
                        facet.summary(),
                        facet.recordedAt()
                ))
                .toList();
    }

    static List<IdentityFacet> toIdentityFacets(List<IdentityFacetNode> nodes) {
        return nodes.stream()
                .map(node -> new IdentityFacet(
                        node.getFacetId(),
                        node.getKind(),
                        node.getSummary(),
                        node.getRecordedAt()
                ))
                .toList();
    }

    static List<RelationshipEntityNode> toRelationshipNodes(List<RelationshipEntity> relationships) {
        return relationships.stream()
                .map(relationship -> new RelationshipEntityNode(
                        relationship.entityId(),
                        relationship.kind(),
                        relationship.displayName(),
                        relationship.recordedAt()
                ))
                .toList();
    }

    static List<RelationshipEntity> toRelationships(List<RelationshipEntityNode> nodes) {
        return nodes.stream()
                .map(node -> new RelationshipEntity(
                        node.getEntityId(),
                        node.getKind(),
                        node.getDisplayName(),
                        node.getRecordedAt()
                ))
                .toList();
    }
}
