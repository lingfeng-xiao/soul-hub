package com.lingfeng.sprite.controller;

import com.lingfeng.sprite.mcp.McpClient;
import com.lingfeng.sprite.mcp.McpServer;
import com.lingfeng.sprite.mcp.McpTool;
import com.lingfeng.sprite.mcp.McpTransport;
import com.lingfeng.sprite.mcp.transports.HttpTransport;
import com.lingfeng.sprite.mcp.transports.StdioTransport;
import com.lingfeng.sprite.skill.SkillRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * REST controller for MCP server/client management
 */
@RestController
@RequestMapping("/api/mcp")
public class McpController {
    private static final Logger logger = LoggerFactory.getLogger(McpController.class);

    private final SkillRegistry skillRegistry;
    private McpServer server;
    private McpClient client;
    private final Map<String, Object> serverConfig = new ConcurrentHashMap<>();
    private final Map<String, Object> clientConfig = new ConcurrentHashMap<>();

    public McpController(SkillRegistry skillRegistry) {
        this.skillRegistry = skillRegistry;
    }

    /**
     * Get MCP server status
     */
    @GetMapping("/server/status")
    public ResponseEntity<Map<String, Object>> getServerStatus() {
        Map<String, Object> response = new HashMap<>();
        if (server != null) {
            response.put("running", server.isRunning());
            response.put("toolCount", server.getToolCount());
            response.put("sessionCount", server.getSessionCount());
        } else {
            response.put("running", false);
        }
        return ResponseEntity.ok(response);
    }

    /**
     * Start MCP server
     */
    @PostMapping("/server/start")
    public ResponseEntity<Map<String, Object>> startServer(
            @RequestParam(defaultValue = "http") String transport,
            @RequestParam(defaultValue = "8081") int port
    ) {
        Map<String, Object> response = new HashMap<>();

        if (server != null && server.isRunning()) {
            response.put("error", "Server already running");
            return ResponseEntity.badRequest().body(response);
        }

        try {
            McpTransport mcpTransport = "stdio".equalsIgnoreCase(transport) ?
                new StdioTransport() : new HttpTransport(port);

            server = new McpServer("sprite", "1.0.0", skillRegistry, mcpTransport);
            server.start();

            // Register built-in resources
            server.registerResource(
                com.lingfeng.sprite.mcp.McpResource.of(
                    "sprite://memory",
                    "Sprite Memory",
                    "Current memory state"
                )
            );
            server.registerResource(
                com.lingfeng.sprite.mcp.McpResource.of(
                    "sprite://status",
                    "Sprite Status",
                    "Current sprite running status"
                )
            );

            response.put("success", true);
            response.put("transport", transport);
            response.put("port", port);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Failed to start MCP server", e);
            response.put("error", e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * Stop MCP server
     */
    @PostMapping("/server/stop")
    public ResponseEntity<Map<String, Object>> stopServer() {
        Map<String, Object> response = new HashMap<>();

        if (server == null || !server.isRunning()) {
            response.put("error", "Server not running");
            return ResponseEntity.badRequest().body(response);
        }

        server.stop();
        server = null;
        response.put("success", true);
        return ResponseEntity.ok(response);
    }

    /**
     * List available tools
     */
    @GetMapping("/tools")
    public ResponseEntity<Map<String, Object>> listTools() {
        Map<String, Object> response = new HashMap<>();
        if (server != null && server.isRunning()) {
            response.put("tools", server.getToolCount());
        } else {
            response.put("tools", 0);
        }
        response.put("skills", skillRegistry.count());
        return ResponseEntity.ok(response);
    }

    /**
     * Get client status
     */
    @GetMapping("/client/status")
    public ResponseEntity<Map<String, Object>> getClientStatus() {
        Map<String, Object> response = new HashMap<>();
        if (client != null) {
            var servers = client.getServers();
            response.put("connectedServers", servers.size());
            response.put("servers", servers.stream()
                .map(s -> Map.of(
                    "name", s.getName(),
                    "uri", s.getUri().toString(),
                    "connected", s.isConnected(),
                    "toolCount", s.getTools().size()
                ))
                .toList());
        } else {
            response.put("connectedServers", 0);
        }
        return ResponseEntity.ok(response);
    }

    /**
     * Connect to remote server
     */
    @PostMapping("/client/connect")
    public ResponseEntity<Map<String, Object>> connect(
            @RequestParam String name,
            @RequestParam String url
    ) {
        Map<String, Object> response = new HashMap<>();

        if (client == null) {
            client = new McpClient("sprite");
        }

        try {
            client.connect(name, url)
                .thenAccept(server -> {
                    response.put("success", server.isConnected());
                    response.put("name", name);
                })
                .exceptionally(e -> {
                    response.put("error", e.getMessage());
                    return null;
                });

            response.put("connecting", true);
            response.put("name", name);
            return ResponseEntity.accepted().body(response);

        } catch (Exception e) {
            logger.error("Error connecting to {}", name, e);
            response.put("error", e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * Disconnect from server
     */
    @PostMapping("/client/disconnect")
    public ResponseEntity<Map<String, Object>> disconnect(@RequestParam String name) {
        Map<String, Object> response = new HashMap<>();

        if (client != null) {
            client.disconnect(name);
            response.put("success", true);
            response.put("name", name);
        } else {
            response.put("error", "Client not initialized");
        }
        return ResponseEntity.ok(response);
    }

    /**
     * List connected servers
     */
    @GetMapping("/client/servers")
    public ResponseEntity<Map<String, Object>> getClientServers() {
        Map<String, Object> response = new HashMap<>();

        if (client != null) {
            var servers = client.getServers();
            response.put("servers", servers.stream()
                .map(s -> Map.of(
                    "name", s.getName(),
                    "uri", s.getUri().toString(),
                    "connected", s.isConnected(),
                    "toolCount", s.getTools().size()
                ))
                .toList());
            response.put("count", servers.size());
        } else {
            response.put("servers", List.of());
            response.put("count", 0);
        }
        return ResponseEntity.ok(response);
    }

    /**
     * List tools from all connected servers
     */
    @GetMapping("/client/tools")
    public ResponseEntity<Map<String, Object>> getClientTools() {
        Map<String, Object> response = new HashMap<>();

        if (client != null) {
            var tools = client.getAllTools();
            response.put("tools", tools.stream()
                .map(t -> Map.of(
                    "name", t.name(),
                    "description", t.description(),
                    "inputSchema", t.inputSchema() != null ? t.inputSchema() : Map.of()
                ))
                .toList());
            response.put("count", tools.size());
            response.put("serverCount", client.getServers().stream().filter(McpClient.RemoteServer::isConnected).count());
        } else {
            response.put("tools", List.of());
            response.put("count", 0);
            response.put("serverCount", 0);
        }
        return ResponseEntity.ok(response);
    }

    /**
     * List tools from a specific server
     */
    @GetMapping("/client/tools/{serverName}")
    public ResponseEntity<Map<String, Object>> getServerTools(@PathVariable String serverName) {
        Map<String, Object> response = new HashMap<>();

        if (client != null) {
            var tools = client.getTools(serverName);
            response.put("server", serverName);
            response.put("tools", tools.stream()
                .map(t -> Map.of(
                    "name", t.name(),
                    "description", t.description(),
                    "inputSchema", t.inputSchema() != null ? t.inputSchema() : Map.of()
                ))
                .toList());
            response.put("count", tools.size());
        } else {
            response.put("error", "Client not initialized");
            return ResponseEntity.badRequest().body(response);
        }
        return ResponseEntity.ok(response);
    }

    /**
     * Health check
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "UP");
        response.put("serverRunning", server != null && server.isRunning());
        response.put("clientInitialized", client != null);
        return ResponseEntity.ok(response);
    }
}
