package com.lingfeng.sprite.mcp.transports;

import com.lingfeng.sprite.mcp.McpTransport;
import com.lingfeng.sprite.mcp.McpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.sun.net.httpserver.*;
import java.net.InetSocketAddress;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.CompletableFuture;

/**
 * HTTP-based MCP transport using built-in HttpServer
 */
public class HttpTransport implements McpTransport {
    private static final Logger logger = LoggerFactory.getLogger(HttpTransport.class);

    private final int port;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private HttpServer server;
    private final Map<String, McpSession> sessions = new ConcurrentHashMap<>();
    private ExecutorService executor;
    private HttpHandler handler;

    public HttpTransport(int port) {
        this.port = port;
    }

    @Override
    public void initialize() throws Exception {
        executor = Executors.newCachedThreadPool();
        logger.info("HttpTransport initialized on port {}", port);
    }

    @Override
    public void start() throws Exception {
        if (!running.compareAndSet(false, true)) {
            return;
        }

        server = HttpServer.create(new InetSocketAddress(port), 0);
        handler = new McpHttpHandler(sessions);
        server.createContext("/mcp", handler);
        server.setExecutor(executor);
        server.start();

        logger.info("HttpTransport started on port {}", port);
    }

    @Override
    public void stop() {
        if (!running.compareAndSet(true, false)) {
            return;
        }

        if (server != null) {
            server.stop(5);
        }
        if (executor != null) {
            executor.shutdown();
        }
        sessions.clear();

        logger.info("HttpTransport stopped");
    }

    @Override
    public CompletableFuture<String> send(String sessionId, String message) {
        return CompletableFuture.completedFuture(message);
    }

    @Override
    public CompletableFuture<Void> broadcast(String message) {
        return CompletableFuture.runAsync(() -> {
            // Broadcast implementation
        });
    }

    @Override
    public boolean isRunning() {
        return running.get();
    }

    @Override
    public TransportType getType() {
        return TransportType.HTTP;
    }

    public int getPort() {
        return port;
    }

    /**
     * HTTP handler for MCP
     */
    private static class McpHttpHandler implements HttpHandler {
        private final Map<String, McpSession> sessions;

        public McpHttpHandler(Map<String, McpSession> sessions) {
            this.sessions = sessions;
        }

        @Override
        public void handle(HttpExchange exchange) {
            try {
                String sessionId = exchange.getRequestHeaders().getFirst("Mcp-Session-Id");
                if (sessionId == null) {
                    sessionId = "http-session-1";
                    sessions.putIfAbsent(sessionId, new McpSession(sessionId));
                }

                String requestBody = new String(exchange.getRequestBody().readAllBytes());

                // Log request
                logger.debug("Received request for session {}: {}", sessionId, requestBody);

                // Send response
                String response = "{\"jsonrpc\":\"2.0\",\"result\":{},\"id\":1}";
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                if (sessionId != null) {
                    exchange.getResponseHeaders().set("Mcp-Session-Id", sessionId);
                }
                exchange.sendResponseHeaders(200, response.length());
                exchange.getResponseBody().write(response.getBytes());

            } catch (Exception e) {
                logger.error("Error handling HTTP request", e);
            } finally {
                exchange.close();
            }
        }
    }
}
