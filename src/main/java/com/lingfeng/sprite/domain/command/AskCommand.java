package com.lingfeng.sprite.domain.command;

import java.time.Instant;
import java.util.Map;

/**
 * AskCommand - 提问命令
 *
 * 用户向 Sprite 提问的命令。
 *
 * 对应 IGN-021
 */
public final class AskCommand implements Command {

    private final String commandId;
    private final String question;
    private final AskOptions options;
    private final Instant createdAt;
    private CommandStatus status;

    private AskCommand(Builder builder) {
        this.commandId = builder.commandId;
        this.question = builder.question;
        this.options = builder.options;
        this.createdAt = builder.createdAt;
        this.status = CommandStatus.PENDING;
    }

    public static AskCommand create(String question) {
        return new Builder()
                .commandId("ask-" + System.currentTimeMillis())
                .question(question)
                .options(new AskOptions(true, 500, Map.of()))
                .createdAt(Instant.now())
                .build();
    }

    @Override
    public String getCommandId() { return commandId; }

    @Override
    public CommandType getType() { return CommandType.ASK; }

    @Override
    public String getDescription() { return question; }

    @Override
    public Instant getCreatedAt() { return createdAt; }

    @Override
    public CommandStatus getStatus() { return status; }

    public String getQuestion() { return question; }

    public AskOptions getOptions() { return options; }

    public void setStatus(CommandStatus status) { this.status = status; }

    public static Builder builder() { return new Builder(); }

    /**
     * 提问选项
     */
    public record AskOptions(
            boolean includeSources,
            int maxLength,
            Map<String, String> context
    ) {
        public AskOptions {
            if (!includeSources) includeSources = true;
            if (maxLength <= 0) maxLength = 500;
            if (context == null) context = Map.of();
        }
    }

    public static final class Builder {
        private String commandId = "";
        private String question = "";
        private AskOptions options = new AskOptions(true, 500, Map.of());
        private Instant createdAt = Instant.now();

        public Builder commandId(String commandId) { this.commandId = commandId; return this; }
        public Builder question(String question) { this.question = question; return this; }
        public Builder options(AskOptions options) { this.options = options; return this; }
        public Builder createdAt(Instant createdAt) { this.createdAt = createdAt; return this; }

        public AskCommand build() { return new AskCommand(this); }
    }
}
