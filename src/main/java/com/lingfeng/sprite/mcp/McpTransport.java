package com.lingfeng.sprite.mcp;

import java.util.concurrent.CompletableFuture;

/**
 * MCP Transport interface
 */
public interface McpTransport {

    /**
     * Initialize the transport
     */
    void initialize() throws Exception;

    /**
     * Start listening for connections
     */
    void start() throws Exception;

    /**
     * Stop the transport
     */
    void stop() throws Exception;

    /**
     * Send a message to a session
     */
    CompletableFuture<String> send(String sessionId, String message);

    /**
     * Broadcast to all sessions
     */
    CompletableFuture<Void> broadcast(String message);

    /**
     * Check if transport is running
     */
    boolean isRunning();

    /**
     * Transport type
     */
    enum TransportType {
        STDIO,
        HTTP
    }

    TransportType getType();
}
