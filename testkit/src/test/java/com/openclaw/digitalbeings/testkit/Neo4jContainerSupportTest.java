package com.openclaw.digitalbeings.testkit;

import org.junit.jupiter.api.Test;
import org.testcontainers.containers.Neo4jContainer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class Neo4jContainerSupportTest {

    @Test
    void createsConfiguredNeo4jContainerWithoutStartingDocker() {
        Neo4jContainer<?> container = Neo4jContainerSupport.createNeo4jContainer();

        assertNotNull(container);
        assertEquals(Neo4jContainerSupport.DEFAULT_ADMIN_PASSWORD, container.getAdminPassword());
        assertEquals(Neo4jContainerSupport.DEFAULT_IMAGE, Neo4jContainerSupport.defaultImageName().asCanonicalNameString());
    }

    @Test
    void validatesConnectionDetailsShape() {
        Neo4jConnectionDetails details = Neo4jContainerSupport.connectionDetails(
                "bolt://localhost:7687",
                Neo4jContainerSupport.DEFAULT_USERNAME,
                Neo4jContainerSupport.DEFAULT_ADMIN_PASSWORD
        );

        assertEquals("bolt://localhost:7687", details.boltUrl());
        assertEquals(Neo4jContainerSupport.DEFAULT_USERNAME, details.username());
        assertEquals(Neo4jContainerSupport.DEFAULT_ADMIN_PASSWORD, details.password());
    }

    @Test
    void sessionCanBeConstructedFromContainerWithoutStartingDocker() {
        Neo4jContainer<?> container = Neo4jContainerSupport.createNeo4jContainer();
        assertNotNull(container);
        assertEquals(Neo4jContainerSupport.DEFAULT_ADMIN_PASSWORD, container.getAdminPassword());
    }
}
