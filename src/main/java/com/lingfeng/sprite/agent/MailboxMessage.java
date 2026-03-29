package com.lingfeng.sprite.agent;

import java.time.Instant;
import java.util.UUID;

/**
 * Inter-agent message format
 */
public record MailboxMessage(
    String id,
    String from,
    String to,
    MessageType type,
    Object payload,
    Instant timestamp,
    boolean acknowledged
) {
    public enum MessageType {
        TASK,       // Leader assigns task to worker
        RESULT,     // Worker returns result
        HEARTBEAT,  // Worker heartbeat
        ERROR,      // Error notification
        SHUTDOWN,   // Shutdown request
        REGISTER,   // Worker registration
        DEREGISTER  // Worker deregistration
    }

    public static MailboxMessage create(String from, String to, MessageType type, Object payload) {
        return new MailboxMessage(
            UUID.randomUUID().toString(),
            from,
            to,
            type,
            payload,
            Instant.now(),
            false
        );
    }

    public MailboxMessage withAcknowledged(boolean acknowledged) {
        return new MailboxMessage(id, from, to, type, payload, timestamp, acknowledged);
    }
}
