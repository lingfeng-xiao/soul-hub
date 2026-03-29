package com.lingfeng.sprite.mcp;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * MCP Session management
 */
public class McpSession {
    private final String sessionId;
    private final Instant createdAt;
    private final Map<String, Object> capabilities = new ConcurrentHashMap<>();
    private final AtomicLong requestId = new AtomicLong(0);
    private volatile boolean closed = false;

    public McpSession(String sessionId) {
        this.sessionId = sessionId;
        this.createdAt = Instant.now();
    }

    public String nextRequestId() {
        return String.valueOf(requestId.incrementAndGet());
    }

    public String getSessionId() {
        return sessionId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCapability(String name, Object value) {
        capabilities.put(name, value);
    }

    public Object getCapability(String name) {
        return capabilities.get(name);
    }

    public boolean hasCapability(String name) {
        return capabilities.containsKey(name);
    }

    public Map<String, Object> getCapabilities() {
        return Map.copyOf(capabilities);
    }

    public boolean isClosed() {
        return closed;
    }

    public void close() {
        this.closed = true;
    }
}
