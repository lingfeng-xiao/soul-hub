package com.lingfeng.sprite.mcp;

import java.util.Map;

/**
 * MCP Tool definition
 */
public record McpTool(
    String name,
    String description,
    Map<String, Object> inputSchema
) {
    /**
     * Create a simple tool with basic schema
     */
    public static McpTool of(String name, String description) {
        return new McpTool(name, description, Map.of(
            "type", "object",
            "properties", Map.of()
        ));
    }

    /**
     * Create a tool with properties
     */
    public static McpTool of(String name, String description, Map<String, ToolProperty> properties) {
        return new McpTool(name, description, Map.of(
            "type", "object",
            "properties", properties
        ));
    }

    /**
     * Tool property definition
     */
    record ToolProperty(
        String type,
        String description,
        boolean required
    ) {
        public static ToolProperty string(String description, boolean required) {
            return new ToolProperty("string", description, required);
        }

        public static ToolProperty number(String description, boolean required) {
            return new ToolProperty("number", description, required);
        }

        public static ToolProperty boolean_(String description, boolean required) {
            return new ToolProperty("boolean", description, required);
        }
    }
}
