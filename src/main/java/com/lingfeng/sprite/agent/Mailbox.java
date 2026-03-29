package com.lingfeng.sprite.agent;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicReference;

/**
 * File-based asynchronous messaging system (ClawTeam pattern)
 *
 * Messages are stored as JSON files in inbox/outbox directories.
 * Uses atomic temp-file-rename pattern for crash safety.
 */
public class Mailbox {
    private static final Logger logger = LoggerFactory.getLogger(Mailbox.class);
    private static final ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());

    private final String basePath;
    private final String agentId;
    private final long pollIntervalMs;
    private final List<MailboxMessage> pendingMessages = new CopyOnWriteArrayList<>();
    private final AtomicReference<Thread> pollThread = new AtomicReference<>();

    public Mailbox(String agentId, String basePath, long pollIntervalMs) {
        this.agentId = agentId;
        this.basePath = basePath;
        this.pollIntervalMs = pollIntervalMs;
        initializeDirectories();
    }

    private void initializeDirectories() {
        Path inbox = Paths.get(basePath, agentId, "inbox");
        Path outbox = Paths.get(basePath, agentId, "outbox");
        try {
            Files.createDirectories(inbox);
            Files.createDirectories(outbox);
            logger.debug("Mailbox initialized for {} at {}", agentId, basePath);
        } catch (IOException e) {
            logger.error("Failed to create mailbox directories for {}", agentId, e);
        }
    }

    /**
     * Send a message to another agent
     */
    public void send(String recipientId, MailboxMessage message) {
        MailboxMessage outgoing = MailboxMessage.create(agentId, recipientId, message.type(), message.payload());
        Path targetPath = Paths.get(basePath, recipientId, "inbox");
        writeMessage(outgoing, targetPath);
        logger.debug("Sent message {} from {} to {}", outgoing.id(), agentId, recipientId);
    }

    /**
     * Send a message to a specific recipient
     */
    public void sendTo(String recipientId, MailboxMessage.MessageType type, Object payload) {
        send(recipientId, MailboxMessage.create(agentId, recipientId, type, payload));
    }

    /**
     * Receive a message (non-blocking)
     */
    public Optional<MailboxMessage> receive() {
        // First check pending
        if (!pendingMessages.isEmpty()) {
            return Optional.of(pendingMessages.remove(0));
        }

        // Then check inbox
        Path inboxPath = Paths.get(basePath, agentId, "inbox");
        List<Path> messages = listMessages(inboxPath);
        if (!messages.isEmpty()) {
            Path messageFile = messages.get(0);
            try {
                String content = Files.readString(messageFile);
                MailboxMessage message = mapper.readValue(content, MailboxMessage.class);
                Files.delete(messageFile);
                logger.debug("Received message {} for {}", message.id(), agentId);
                return Optional.of(message);
            } catch (IOException e) {
                logger.error("Failed to read message from {}", messageFile, e);
            }
        }

        return Optional.empty();
    }

    /**
     * Receive all pending messages
     */
    public List<MailboxMessage> receiveAll() {
        List<MailboxMessage> messages = new ArrayList<>();
        receiveAllFromInbox().forEach(messages::add);
        pendingMessages.forEach(m -> {
            messages.add(m);
            pendingMessages.remove(m);
        });
        return messages;
    }

    private List<MailboxMessage> receiveAllFromInbox() {
        List<MailboxMessage> messages = new ArrayList<>();
        Path inboxPath = Paths.get(basePath, agentId, "inbox");
        List<Path> messageFiles = listMessages(inboxPath);
        for (Path file : messageFiles) {
            try {
                String content = Files.readString(file);
                MailboxMessage message = mapper.readValue(content, MailboxMessage.class);
                Files.delete(file);
                messages.add(message);
            } catch (IOException e) {
                logger.error("Failed to read message from {}", file, e);
            }
        }
        return messages;
    }

    /**
     * Acknowledge a message
     */
    public void ack(String messageId) {
        logger.debug("Message {} acknowledged by {}", messageId, agentId);
    }

    /**
     * Broadcast a message to all agents in the workspace
     */
    public void broadcast(MailboxMessage.MessageType type, Object payload) {
        Path workspaceDir = Paths.get(basePath).getParent();
        if (workspaceDir == null || !Files.exists(workspaceDir)) return;

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(workspaceDir)) {
            for (Path agentPath : stream) {
                if (Files.isDirectory(agentPath) && !agentPath.getFileName().toString().equals(agentId)) {
                    String targetAgent = agentPath.getFileName().toString();
                    sendTo(targetAgent, type, payload);
                }
            }
        } catch (IOException e) {
            logger.error("Failed to broadcast message from {}", agentId, e);
        }
    }

    /**
     * Start polling for messages in background
     */
    public void startPolling(java.util.function.Consumer<MailboxMessage> handler) {
        Thread poller = new Thread(() -> {
            logger.info("Mailbox polling started for {}", agentId);
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    List<MailboxMessage> messages = receiveAll();
                    for (MailboxMessage msg : messages) {
                        handler.accept(msg);
                    }
                    if (messages.isEmpty()) {
                        Thread.sleep(pollIntervalMs);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
            logger.info("Mailbox polling stopped for {}", agentId);
        }, "mailbox-poller-" + agentId);

        poller.setDaemon(true);
        poller.start();
        pollThread.set(poller);
    }

    /**
     * Stop polling
     */
    public void stopPolling() {
        Thread poller = pollThread.getAndSet(null);
        if (poller != null) {
            poller.interrupt();
        }
    }

    private void writeMessage(MailboxMessage message, Path directory) {
        try {
            if (!Files.exists(directory)) {
                Files.createDirectories(directory);
            }
            Path tempFile = directory.resolve(UUID.randomUUID() + ".tmp");
            Path targetFile = directory.resolve(message.id() + ".json");
            String json = mapper.writeValueAsString(message);
            Files.writeString(tempFile, json);
            Files.move(tempFile, targetFile, StandardCopyOption.ATOMIC_MOVE);
            logger.debug("Wrote message {} to {}", message.id(), targetFile);
        } catch (IOException e) {
            logger.error("Failed to write message {}", message.id(), e);
        }
    }

    private List<Path> listMessages(Path directory) {
        List<Path> messages = new ArrayList<>();
        if (!Files.exists(directory)) {
            return messages;
        }
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(directory, "*.json")) {
            for (Path file : stream) {
                messages.add(file);
            }
        } catch (IOException e) {
            logger.error("Failed to list messages in {}", directory, e);
        }
        return messages;
    }

    public String getAgentId() {
        return agentId;
    }

    public String getBasePath() {
        return basePath;
    }
}
