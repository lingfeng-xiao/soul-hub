package com.lingfeng.sprite.mcp;

import java.time.Instant;
import java.util.Map;

/**
 * MCP Resource definition
 */
public record McpResource(
    String uri,
    String name,
    String description,
    String mimeType
) {
    /**
     * Create a simple resource
     */
    public static McpResource of(String uri, String name, String description) {
        return new McpResource(uri, name, description, "application/json");
    }

    /**
     * Resource content
     */
    public record ResourceContent(
        String uri,
        String mimeType,
        Object content
    ) {}

    /**
     * Resource list result
     */
    public record ResourceList(
        java.util.List<McpResource> resources
    ) {}
}
