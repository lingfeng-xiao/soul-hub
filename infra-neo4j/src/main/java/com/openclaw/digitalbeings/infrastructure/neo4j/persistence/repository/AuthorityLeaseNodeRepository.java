package com.openclaw.digitalbeings.infrastructure.neo4j.persistence.repository;

import com.openclaw.digitalbeings.infrastructure.neo4j.persistence.entity.AuthorityLeaseNode;
import java.util.Optional;
import org.springframework.data.neo4j.repository.Neo4jRepository;

public interface AuthorityLeaseNodeRepository extends Neo4jRepository<AuthorityLeaseNode, String> {

    Optional<AuthorityLeaseNode> findByLeaseId(String leaseId);
}
