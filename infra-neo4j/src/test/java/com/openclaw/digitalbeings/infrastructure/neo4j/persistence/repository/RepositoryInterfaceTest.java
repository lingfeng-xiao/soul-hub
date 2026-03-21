package com.openclaw.digitalbeings.infrastructure.neo4j.persistence.repository;

import org.junit.jupiter.api.Test;
import org.springframework.data.neo4j.repository.Neo4jRepository;

import static org.junit.jupiter.api.Assertions.assertTrue;

class RepositoryInterfaceTest {

    @Test
    void repositoriesExtendNeo4jRepository() {
        assertTrue(Neo4jRepository.class.isAssignableFrom(BeingNodeRepository.class));
        assertTrue(Neo4jRepository.class.isAssignableFrom(AuthorityLeaseNodeRepository.class));
        assertTrue(Neo4jRepository.class.isAssignableFrom(ReviewItemNodeRepository.class));
        assertTrue(Neo4jRepository.class.isAssignableFrom(DomainEventNodeRepository.class));
    }
}
