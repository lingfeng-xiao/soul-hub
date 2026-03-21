package com.openclaw.digitalbeings.testkit;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RemoteNeo4jConnectionSupportTest {

    @Test
    void loadsTheLocalRemoteVerificationCredentialsCache() {
        assertTrue(RemoteNeo4jConnectionSupport.isConfigured());

        Neo4jConnectionDetails details = RemoteNeo4jConnectionSupport.requireConnectionDetails();
        assertEquals("bolt://114.67.156.250:17687", details.boltUrl());
        assertEquals("neo4j", details.username());
        assertFalse(details.password().isBlank());
    }
}
