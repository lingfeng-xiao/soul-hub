package com.lingfeng.sprite.domain.command;

import java.time.Instant;

/**
 * ResearchCommand - 研究命令
 *
 * 用户向 Sprite 发送研究请求的命令。
 *
 * 对应 IGN-021
 */
public final class ResearchCommand implements Command {

    private final String commandId;
    private final String topic;
    private final ResearchScope scope;
    private final Instant createdAt;
    private CommandStatus status;

    private ResearchCommand(Builder builder) {
        this.commandId = builder.commandId;
        this.topic = builder.topic;
        this.scope = builder.scope;
        this.createdAt = builder.createdAt;
        this.status = CommandStatus.PENDING;
    }

    public static ResearchCommand create(String topic) {
        return new Builder()
                .commandId("research-" + System.currentTimeMillis())
                .topic(topic)
                .scope(ResearchScope.NORMAL)
                .createdAt(Instant.now())
                .build();
    }

    @Override
    public String getCommandId() { return commandId; }

    @Override
    public CommandType getType() { return CommandType.RESEARCH; }

    @Override
    public String getDescription() { return "Research: " + topic; }

    @Override
    public Instant getCreatedAt() { return createdAt; }

    @Override
    public CommandStatus getStatus() { return status; }

    public String getTopic() { return topic; }

    public ResearchScope getScope() { return scope; }

    public void setStatus(CommandStatus status) { this.status = status; }

    public static Builder builder() { return new Builder(); }

    public enum ResearchScope {
        QUICK,     // 快速概览
        NORMAL,    // 一般研究
        DEEP       // 深度研究
    }

    public static final class Builder {
        private String commandId = "";
        private String topic = "";
        private ResearchScope scope = ResearchScope.NORMAL;
        private Instant createdAt = Instant.now();

        public Builder commandId(String commandId) { this.commandId = commandId; return this; }
        public Builder topic(String topic) { this.topic = topic; return this; }
        public Builder scope(ResearchScope scope) { this.scope = scope; return this; }
        public Builder createdAt(Instant createdAt) { this.createdAt = createdAt; return this; }

        public ResearchCommand build() { return new ResearchCommand(this); }
    }
}
