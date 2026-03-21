package com.openclaw.digitalbeings.testkit;

import org.testcontainers.containers.Neo4jContainer;

public final class Neo4jContainerSession implements AutoCloseable {

    private final Neo4jContainer<?> container;
    private final Neo4jConnectionDetails connectionDetails;

    private Neo4jContainerSession(Neo4jContainer<?> container) {
        this.container = container;
        this.connectionDetails = Neo4jContainerSupport.connectionDetails(
                container.getBoltUrl(),
                Neo4jContainerSupport.DEFAULT_USERNAME,
                container.getAdminPassword()
        );
    }

    public static Neo4jContainerSession start() {
        if (!Neo4jContainerSupport.isDockerAvailable()) {
            throw new IllegalStateException("Docker is not available on this machine.");
        }
        Neo4jContainer<?> container = Neo4jContainerSupport.createNeo4jContainer();
        container.start();
        return new Neo4jContainerSession(container);
    }

    public Neo4jContainer<?> container() {
        return container;
    }

    public Neo4jConnectionDetails connectionDetails() {
        return connectionDetails;
    }

    @Override
    public void close() {
        container.stop();
    }
}
