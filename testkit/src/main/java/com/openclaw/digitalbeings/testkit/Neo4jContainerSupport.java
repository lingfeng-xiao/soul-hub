package com.openclaw.digitalbeings.testkit;

import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.Neo4jContainer;
import org.testcontainers.utility.DockerImageName;

public final class Neo4jContainerSupport {

    public static final String DEFAULT_IMAGE = "neo4j:5.25";
    public static final String DEFAULT_ADMIN_PASSWORD = "test-password";
    public static final String DEFAULT_USERNAME = "neo4j";
    public static final String PAGE_CACHE_SIZE = "256M";
    public static final String HEAP_INITIAL_SIZE = "256M";
    public static final String HEAP_MAX_SIZE = "512M";

    private Neo4jContainerSupport() {
    }

    public static DockerImageName defaultImageName() {
        return DockerImageName.parse(DEFAULT_IMAGE);
    }

    public static Neo4jContainer<?> createNeo4jContainer() {
        return new Neo4jContainer<>(defaultImageName())
                .withAdminPassword(DEFAULT_ADMIN_PASSWORD)
                .withNeo4jConfig("server.memory.pagecache.size", PAGE_CACHE_SIZE)
                .withNeo4jConfig("server.memory.heap.initial_size", HEAP_INITIAL_SIZE)
                .withNeo4jConfig("server.memory.heap.max_size", HEAP_MAX_SIZE);
    }

    public static boolean isDockerAvailable() {
        return DockerClientFactory.instance().isDockerAvailable();
    }

    public static Neo4jConnectionDetails connectionDetails(String boltUrl, String username, String password) {
        return new Neo4jConnectionDetails(boltUrl, username, password);
    }
}
