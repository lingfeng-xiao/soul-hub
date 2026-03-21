package com.openclaw.digitalbeings.infrastructure.neo4j.persistence.repository;

import com.openclaw.digitalbeings.infrastructure.neo4j.persistence.entity.DomainEventNode;
import java.util.Optional;
import org.springframework.data.neo4j.repository.Neo4jRepository;

public interface DomainEventNodeRepository extends Neo4jRepository<DomainEventNode, String> {

    Optional<DomainEventNode> findByEventId(String eventId);
}
