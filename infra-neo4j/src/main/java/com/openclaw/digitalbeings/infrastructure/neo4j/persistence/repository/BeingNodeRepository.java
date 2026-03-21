package com.openclaw.digitalbeings.infrastructure.neo4j.persistence.repository;

import com.openclaw.digitalbeings.infrastructure.neo4j.persistence.entity.BeingNode;
import java.util.Optional;
import org.springframework.data.neo4j.repository.Neo4jRepository;

public interface BeingNodeRepository extends Neo4jRepository<BeingNode, String> {

    Optional<BeingNode> findByBeingId(String beingId);
}
