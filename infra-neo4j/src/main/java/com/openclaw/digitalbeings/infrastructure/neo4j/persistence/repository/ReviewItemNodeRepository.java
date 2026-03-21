package com.openclaw.digitalbeings.infrastructure.neo4j.persistence.repository;

import com.openclaw.digitalbeings.infrastructure.neo4j.persistence.entity.ReviewItemNode;
import java.util.Optional;
import org.springframework.data.neo4j.repository.Neo4jRepository;

public interface ReviewItemNodeRepository extends Neo4jRepository<ReviewItemNode, String> {

    Optional<ReviewItemNode> findByReviewItemId(String reviewItemId);
}
