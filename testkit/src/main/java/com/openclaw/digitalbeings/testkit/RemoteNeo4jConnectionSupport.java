package com.openclaw.digitalbeings.testkit;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import org.neo4j.driver.AuthTokens;
import org.neo4j.driver.Driver;
import org.neo4j.driver.GraphDatabase;

public final class RemoteNeo4jConnectionSupport {

    private static final String ENV_FILE_NAME = ".local/remote-verification.env";

    private RemoteNeo4jConnectionSupport() {
    }

    public static Optional<Neo4jConnectionDetails> loadConnectionDetails() {
        Optional<Path> envFile = locateEnvFile();
        if (envFile.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(parseConnectionDetails(envFile.get()));
    }

    public static boolean isConfigured() {
        return loadConnectionDetails().isPresent();
    }

    public static Neo4jConnectionDetails requireConnectionDetails() {
        return loadConnectionDetails()
                .orElseThrow(() -> new IllegalStateException("Missing remote Neo4j credential cache at " + fallbackEnvPath()));
    }

    public static Driver openDriver() {
        Neo4jConnectionDetails details = requireConnectionDetails();
        return GraphDatabase.driver(details.boltUrl(), AuthTokens.basic(details.username(), details.password()));
    }

    public static boolean canAuthenticate() {
        try (Driver driver = openDriver()) {
            try (var session = driver.session()) {
                session.run("RETURN 1 AS ok").consume();
                return true;
            }
        } catch (RuntimeException exception) {
            return false;
        }
    }

    public static Path workspaceRoot() {
        return locateEnvFile()
                .map(Path::getParent)
                .map(Path::getParent)
                .orElseGet(() -> Paths.get(System.getProperty("user.dir")).toAbsolutePath().normalize());
    }

    private static Optional<Path> locateEnvFile() {
        Path current = Paths.get(System.getProperty("user.dir")).toAbsolutePath().normalize();
        while (current != null) {
            Path candidate = current.resolve(ENV_FILE_NAME);
            if (Files.isRegularFile(candidate)) {
                return Optional.of(candidate);
            }
            current = current.getParent();
        }
        return Optional.empty();
    }

    private static Path fallbackEnvPath() {
        return workspaceRoot().resolve(ENV_FILE_NAME);
    }

    private static Neo4jConnectionDetails parseConnectionDetails(Path envFile) {
        Map<String, String> entries = new LinkedHashMap<>();
        try {
            for (String line : Files.readAllLines(envFile, StandardCharsets.UTF_8)) {
                String trimmed = line.trim();
                if (trimmed.isEmpty() || trimmed.startsWith("#") || !trimmed.contains("=")) {
                    continue;
                }
                int separatorIndex = trimmed.indexOf('=');
                String key = trimmed.substring(0, separatorIndex).trim();
                String value = trimmed.substring(separatorIndex + 1).trim();
                entries.put(key, value);
            }
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to read remote Neo4j credentials from " + envFile, exception);
        }

        String host = require(entries, "DBJ_REMOTE_HOST");
        String username = entries.getOrDefault("DBJ_REMOTE_NEO4J_USERNAME", "neo4j");
        String password = require(entries, "DBJ_REMOTE_NEO4J_PASSWORD");
        String boltPort = require(entries, "DBJ_REMOTE_NEO4J_BOLT_PORT");
        return new Neo4jConnectionDetails("bolt://" + host + ":" + boltPort, username, password);
    }

    private static String require(Map<String, String> entries, String key) {
        String value = entries.get(key);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Missing required remote Neo4j entry: " + key);
        }
        return value.trim();
    }
}
