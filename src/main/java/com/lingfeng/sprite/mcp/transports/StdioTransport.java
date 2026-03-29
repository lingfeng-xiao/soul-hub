package com.lingfeng.sprite.mcp.transports;

import com.lingfeng.sprite.mcp.McpTransport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.concurrent.CompletableFuture;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Stdio-based MCP transport
 */
public class StdioTransport implements McpTransport {
    private static final Logger logger = LoggerFactory.getLogger(StdioTransport.class);

    private final AtomicBoolean running = new AtomicBoolean(false);
    private final Map<String, StdioSession> sessions = new ConcurrentHashMap<>();
    private BufferedReader reader;
    private PrintWriter writer;
    private Thread readThread;

    @Override
    public void initialize() throws Exception {
        reader = new BufferedReader(new InputStreamReader(System.in));
        writer = new PrintWriter(System.out, true);
        logger.info("StdioTransport initialized");
    }

    @Override
    public void start() throws Exception {
        if (!running.compareAndSet(false, true)) {
            return;
        }

        String sessionId = "stdio-session-1";
        sessions.put(sessionId, new StdioSession(sessionId, reader, writer));

        readThread = new Thread(() -> {
            logger.info("StdioTransport read thread started");
            try {
                String line;
                while (running.get() && (line = reader.readLine()) != null) {
                    logger.debug("Received: {}", line);
                    StdioSession session = sessions.get(sessionId);
                    if (session != null) {
                        session.handleMessage(line);
                    }
                }
            } catch (Exception e) {
                if (running.get()) {
                    logger.error("Error reading from stdin", e);
                }
            }
            logger.info("StdioTransport read thread ended");
        }, "stdio-reader");
        readThread.setDaemon(true);
        readThread.start();

        logger.info("StdioTransport started");
    }

    @Override
    public void stop() {
        if (!running.compareAndSet(true, false)) {
            return;
        }

        sessions.values().forEach(StdioSession::close);
        sessions.clear();

        if (readThread != null) {
            readThread.interrupt();
        }

        logger.info("StdioTransport stopped");
    }

    @Override
    public CompletableFuture<String> send(String sessionId, String message) {
        return CompletableFuture.supplyAsync(() -> {
            StdioSession session = sessions.get(sessionId);
            if (session != null) {
                session.send(message);
            }
            return message;
        });
    }

    @Override
    public CompletableFuture<Void> broadcast(String message) {
        return CompletableFuture.runAsync(() -> {
            sessions.values().forEach(s -> s.send(message));
        });
    }

    @Override
    public boolean isRunning() {
        return running.get();
    }

    @Override
    public TransportType getType() {
        return TransportType.STDIO;
    }

    /**
     * Internal session for stdio
     */
    private static class StdioSession {
        private final String id;
        private final BufferedReader reader;
        private final PrintWriter writer;
        private final ConcurrentHashMap<String, MessageHandler> handlers = new ConcurrentHashMap<>();

        public StdioSession(String id, BufferedReader reader, PrintWriter writer) {
            this.id = id;
            this.reader = reader;
            this.writer = writer;
        }

        public void send(String message) {
            writer.println(message);
            writer.flush();
        }

        public void handleMessage(String message) {
            // Dispatch to registered handlers
            handlers.values().forEach(h -> h.handle(message));
        }

        public void onMessage(MessageHandler handler) {
            handlers.put(handler.getClass().getName(), handler);
        }

        public void close() {
            handlers.clear();
        }

        public interface MessageHandler {
            void handle(String message);
        }
    }
}
