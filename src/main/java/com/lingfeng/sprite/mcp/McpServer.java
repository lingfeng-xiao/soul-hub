package com.lingfeng.sprite.mcp;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.lingfeng.sprite.mcp.codec.JsonRpcCodec;
import com.lingfeng.sprite.mcp.transports.HttpTransport;
import com.lingfeng.sprite.mcp.transports.StdioTransport;
import com.lingfeng.sprite.skill.Skill;
import com.lingfeng.sprite.skill.SkillContext;
import com.lingfeng.sprite.skill.SkillRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Sprite as MCP Server - provides tools to external AI clients
 */
public class McpServer {
    private static final Logger logger = LoggerFactory.getLogger(McpServer.class);
    private static final ObjectMapper mapper = new ObjectMapper();

    private final String serverName;
    private final String serverVersion;
    private final SkillRegistry skillRegistry;
    private final McpTransport transport;
    private final Map<String, McpSession> sessions = new ConcurrentHashMap<>();
    private final List<McpTool> tools = new ArrayList<>();
    private final List<McpResource> resources = new ArrayList<>();

    private volatile boolean initialized = false;

    public McpServer(String serverName, String serverVersion, SkillRegistry skillRegistry, McpTransport transport) {
        this.serverName = serverName;
        this.serverVersion = serverVersion;
        this.skillRegistry = skillRegistry;
        this.transport = transport;
    }

    /**
     * Initialize and start the MCP server
     */
    public void start() throws Exception {
        if (initialized) {
            logger.warn("McpServer already initialized");
            return;
        }

        transport.initialize();
        transport.start();
        initialized = true;

        logger.info("McpServer '{}' v{} started with {} transport",
            serverName, serverVersion, transport.getType());
    }

    /**
     * Stop the MCP server
     */
    public void stop() {
        if (!initialized) {
            return;
        }

        try {
            transport.stop();
        } catch (Exception e) {
            logger.warn("Error stopping transport", e);
        }
        sessions.clear();
        initialized = false;

        logger.info("McpServer stopped");
    }

    /**
     * Register a tool with the server
     */
    public void registerTool(McpTool tool) {
        tools.add(tool);
        logger.debug("Registered tool: {}", tool.name());
    }

    /**
     * Register a resource with the server
     */
    public void registerResource(McpResource resource) {
        resources.add(resource);
        logger.debug("Registered resource: {}", resource.uri());
    }

    /**
     * Handle incoming JSON-RPC message
     */
    public CompletableFuture<String> handleMessage(String sessionId, String message) {
        if (!initialized) {
            return CompletableFuture.completedFuture(
                JsonRpcCodec.encodeError(-32000, "Server not initialized", null));
        }

        try {
            JsonRpcCodec.JsonRpcMessage msg = JsonRpcCodec.parse(message);

            if (msg.isError()) {
                return CompletableFuture.completedFuture(
                    JsonRpcCodec.encodeError(msg.getError().code, msg.getError().message, null));
            }

            String method = msg.getMethod();
            Object params = msg.getParams();
            Object id = msg.getId();

            return switch (method) {
                case "initialize" -> handleInitialize(params, id);
                case "tools/list" -> handleToolsList(id);
                case "tools/call" -> handleToolsCall(params, id);
                case "resources/list" -> handleResourcesList(id);
                case "resources/read" -> handleResourcesRead(params, id);
                default -> CompletableFuture.completedFuture(
                    JsonRpcCodec.encodeError(-32601, "Method not found: " + method, id));
            };
        } catch (Exception e) {
            logger.error("Error handling message", e);
            return CompletableFuture.completedFuture(
                JsonRpcCodec.encodeError(-32603, "Internal error: " + e.getMessage(), null));
        }
    }

    private CompletableFuture<String> handleInitialize(Object params, Object id) {
        ObjectNode result = mapper.createObjectNode();
        result.put("protocolVersion", "2024-11-05");
        ObjectNode serverInfo = result.putObject("serverInfo");
        serverInfo.put("name", serverName);
        serverInfo.put("version", serverVersion);
        result.put("capabilities", mapper.createObjectNode()
            .put("tools", true)
            .put("resources", true));

        // Register session
        String sessionId = "session-" + System.currentTimeMillis();
        sessions.put(sessionId, new McpSession(sessionId));

        logger.info("Client initialized: {} v{}", serverName, serverVersion);
        return CompletableFuture.completedFuture(JsonRpcCodec.encodeResponse(result, String.valueOf(id)));
    }

    private CompletableFuture<String> handleToolsList(Object id) {
        ObjectNode result = mapper.createObjectNode();
        ArrayNode toolsArray = result.putArray("tools");

        for (McpTool tool : tools) {
            ObjectNode toolNode = toolsArray.addObject();
            toolNode.put("name", tool.name());
            toolNode.put("description", tool.description());
            toolNode.set("inputSchema", mapper.valueToTree(tool.inputSchema()));
        }

        // Also add skills from registry
        for (Skill skill : skillRegistry.getAll()) {
            ObjectNode toolNode = toolsArray.addObject();
            toolNode.put("name", skill.id());
            toolNode.put("description", skill.description());
            toolNode.putObject("inputSchema");
        }

        return CompletableFuture.completedFuture(JsonRpcCodec.encodeResponse(result, String.valueOf(id)));
    }

    private CompletableFuture<String> handleToolsCall(Object params, Object id) {
        try {
            if (!(params instanceof Map)) {
                return CompletableFuture.completedFuture(
                    JsonRpcCodec.encodeError(-32602, "Invalid params", String.valueOf(id)));
            }

            @SuppressWarnings("unchecked")
            Map<String, Object> paramsMap = (Map<String, Object>) params;
            String toolName = (String) paramsMap.get("name");
            @SuppressWarnings("unchecked")
            Map<String, Object> arguments = (Map<String, Object>) paramsMap.get("arguments");

            if (toolName == null) {
                return CompletableFuture.completedFuture(
                    JsonRpcCodec.encodeError(-32602, "Tool name is required", String.valueOf(id)));
            }

            // Find skill by name
            Optional<Skill> skillOpt = skillRegistry.get(toolName);
            if (skillOpt.isEmpty()) {
                return CompletableFuture.completedFuture(
                    JsonRpcCodec.encodeError(-32602, "Tool not found: " + toolName, String.valueOf(id)));
            }

            Skill skill = skillOpt.get();
            SkillContext context = SkillContext.create(
                "mcp-" + System.currentTimeMillis(),
                arguments != null ? arguments : Map.of(),
                null, null, null
            );

            var skillResult = skill.execute(context);

            ObjectNode result = mapper.createObjectNode();
            ArrayNode content = result.putArray("content");
            ObjectNode textContent = content.addObject();
            textContent.put("type", "text");
            textContent.put("text", skillResult.success() ?
                mapper.writeValueAsString(skillResult.data()) :
                "Error: " + skillResult.message());

            result.put("isError", Boolean.valueOf(!skillResult.success()));

            return CompletableFuture.completedFuture(JsonRpcCodec.encodeResponse(result, id));

        } catch (Exception e) {
            logger.error("Error calling tool", e);
            return CompletableFuture.completedFuture(
                JsonRpcCodec.encodeError(-32603, "Tool execution failed: " + e.getMessage(), String.valueOf(id)));
        }
    }

    private CompletableFuture<String> handleResourcesList(Object id) {
        ObjectNode result = mapper.createObjectNode();
        ArrayNode resourcesArray = result.putArray("resources");

        for (McpResource resource : resources) {
            ObjectNode resourceNode = resourcesArray.addObject();
            resourceNode.put("uri", resource.uri());
            resourceNode.put("name", resource.name());
            resourceNode.put("description", resource.description());
            resourceNode.put("mimeType", resource.mimeType());
        }

        return CompletableFuture.completedFuture(JsonRpcCodec.encodeResponse(result, String.valueOf(id)));
    }

    private CompletableFuture<String> handleResourcesRead(Object params, Object id) {
        try {
            if (!(params instanceof Map)) {
                return CompletableFuture.completedFuture(
                    JsonRpcCodec.encodeError(-32602, "Invalid params", String.valueOf(id)));
            }

            @SuppressWarnings("unchecked")
            Map<String, Object> paramsMap = (Map<String, Object>) params;
            String uri = (String) paramsMap.get("uri");

            ObjectNode result = mapper.createObjectNode();
            ArrayNode contents = result.putArray("contents");
            ObjectNode content = contents.addObject();
            content.put("uri", uri);
            content.put("mimeType", "application/json");
            content.put("text", "{\"status\": \"Resource content would be here\"}");

            return CompletableFuture.completedFuture(JsonRpcCodec.encodeResponse(result, String.valueOf(id)));

        } catch (Exception e) {
            logger.error("Error reading resource", e);
            return CompletableFuture.completedFuture(
                JsonRpcCodec.encodeError(-32603, "Resource read failed: " + e.getMessage(), String.valueOf(id)));
        }
    }

    public boolean isRunning() {
        return initialized && transport.isRunning();
    }

    public int getSessionCount() {
        return sessions.size();
    }

    public int getToolCount() {
        return tools.size();
    }
}
