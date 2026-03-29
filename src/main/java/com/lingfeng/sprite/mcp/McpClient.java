package com.lingfeng.sprite.mcp;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lingfeng.sprite.mcp.codec.JsonRpcCodec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * Sprite as MCP Client - calls external MCP servers
 */
public class McpClient {
    private static final Logger logger = LoggerFactory.getLogger(McpClient.class);
    private static final ObjectMapper mapper = new ObjectMapper();

    private final String clientName;
    private final Map<String, RemoteServer> servers = new HashMap<>();
    private HttpClient httpClient;

    public McpClient(String clientName) {
        this.clientName = clientName;
        this.httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();
    }

    /**
     * Remote MCP server info
     */
    public static class RemoteServer {
        private final String name;
        private final URI uri;
        private final String sessionId;
        private volatile List<McpTool> tools = new ArrayList<>();
        private volatile Map<String, McpResource> resources = new HashMap<>();
        private volatile boolean connected = false;

        public RemoteServer(String name, URI uri) {
            this.name = name;
            this.uri = uri;
            this.sessionId = "client-" + UUID.randomUUID();
        }

        public String getName() { return name; }
        public URI getUri() { return uri; }
        public String getSessionId() { return sessionId; }
        public List<McpTool> getTools() { return tools; }
        public Map<String, McpResource> getResources() { return resources; }
        public boolean isConnected() { return connected; }
        public void setConnected(boolean connected) { this.connected = connected; }
    }

    /**
     * Connect to a remote MCP server
     */
    public CompletableFuture<RemoteServer> connect(String name, String serverUrl) {
        try {
            URI uri = URI.create(serverUrl);
            RemoteServer server = new RemoteServer(name, uri);
            servers.put(name, server);

            // Send initialize request
            String initRequest = JsonRpcCodec.encodeRequest("initialize", Map.of(
                "protocolVersion", "2024-11-05",
                "clientInfo", Map.of(
                    "name", clientName,
                    "version", "1.0.0"
                ),
                "capabilities", Map.of()
            ), "1");

            HttpRequest request = HttpRequest.newBuilder()
                .uri(uri)
                .header("Content-Type", "application/json")
                .header("Mcp-Session-Id", server.getSessionId())
                .POST(HttpRequest.BodyPublishers.ofString(initRequest))
                .timeout(Duration.ofSeconds(30))
                .build();

            return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> {
                    if (response.statusCode() == 200) {
                        server.setConnected(true);
                        logger.info("Connected to MCP server: {}", name);
                    } else {
                        logger.warn("Failed to connect to {}: status {}", name, response.statusCode());
                    }
                    return server;
                })
                .exceptionally(e -> {
                    logger.error("Error connecting to {}: {}", name, e.getMessage());
                    return server;
                });

        } catch (Exception e) {
            logger.error("Error connecting to {}: {}", name, e.getMessage());
            return CompletableFuture.failedFuture(e);
        }
    }

    /**
     * Disconnect from a server
     */
    public void disconnect(String serverName) {
        RemoteServer server = servers.remove(serverName);
        if (server != null) {
            server.setConnected(false);
            logger.info("Disconnected from MCP server: {}", serverName);
        }
    }

    /**
     * List available tools from a server
     */
    public CompletableFuture<List<McpTool>> listTools(String serverName) {
        RemoteServer server = servers.get(serverName);
        if (server == null || !server.isConnected()) {
            return CompletableFuture.completedFuture(List.of());
        }

        String request = JsonRpcCodec.encodeRequest("tools/list", Map.of(), "2");

        return sendRequest(server, request)
            .thenApply(response -> {
                try {
                    JsonRpcCodec.JsonRpcMessage msg = JsonRpcCodec.parse(response);
                    if (msg.getResult() != null) {
                        // Parse tools from result
                        List<McpTool> tools = new ArrayList<>();
                        // Simplified parsing
                        return tools;
                    }
                } catch (Exception e) {
                    logger.error("Error parsing tools list", e);
                }
                return List.of();
            });
    }

    /**
     * Call a tool on a server
     */
    public CompletableFuture<String> callTool(String serverName, String toolName, Map<String, Object> arguments) {
        RemoteServer server = servers.get(serverName);
        if (server == null || !server.isConnected()) {
            return CompletableFuture.failedFuture(new IllegalStateException("Not connected to " + serverName));
        }

        String request = JsonRpcCodec.encodeRequest("tools/call", Map.of(
            "name", toolName,
            "arguments", arguments != null ? arguments : Map.of()
        ), "3");

        return sendRequest(server, request);
    }

    private CompletableFuture<String> sendRequest(RemoteServer server, String request) {
        HttpRequest httpRequest = HttpRequest.newBuilder()
            .uri(server.getUri())
            .header("Content-Type", "application/json")
            .header("Mcp-Session-Id", server.getSessionId())
            .POST(HttpRequest.BodyPublishers.ofString(request))
            .timeout(Duration.ofSeconds(60))
            .build();

        return httpClient.sendAsync(httpRequest, HttpResponse.BodyHandlers.ofString())
            .thenApply(response -> {
                if (response.statusCode() == 200) {
                    return response.body();
                } else {
                    throw new RuntimeException("HTTP error: " + response.statusCode());
                }
            });
    }

    /**
     * Get connected servers
     */
    public Collection<RemoteServer> getServers() {
        return servers.values();
    }

    /**
     * Check if connected to a server
     */
    public boolean isConnected(String serverName) {
        RemoteServer server = servers.get(serverName);
        return server != null && server.isConnected();
    }

    /**
     * Get all tools from all connected servers
     */
    public List<McpTool> getAllTools() {
        List<McpTool> allTools = new ArrayList<>();
        for (RemoteServer server : servers.values()) {
            if (server.isConnected()) {
                allTools.addAll(server.getTools());
            }
        }
        return allTools;
    }

    /**
     * Get tools from a specific server
     */
    public List<McpTool> getTools(String serverName) {
        RemoteServer server = servers.get(serverName);
        if (server == null || !server.isConnected()) {
            return List.of();
        }
        return server.getTools();
    }
}
