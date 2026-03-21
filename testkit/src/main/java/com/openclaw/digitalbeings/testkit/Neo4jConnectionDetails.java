package com.openclaw.digitalbeings.testkit;

public record Neo4jConnectionDetails(
        String boltUrl,
        String username,
        String password
) {

    public Neo4jConnectionDetails {
        if (boltUrl == null || boltUrl.isBlank()) {
            throw new IllegalArgumentException("boltUrl must not be blank.");
        }
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("username must not be blank.");
        }
        if (password == null || password.isBlank()) {
            throw new IllegalArgumentException("password must not be blank.");
        }
    }
}
