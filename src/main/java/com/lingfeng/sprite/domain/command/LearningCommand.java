package com.lingfeng.sprite.domain.command;

import java.time.Instant;

/**
 * LearningCommand - 学习命令
 *
 * 用户请求 Sprite 提供学习支持的命令。
 *
 * 对应 IGN-021
 */
public final class LearningCommand implements Command {

    private final String commandId;
    private final String learningGoal;
    private final LearningContext context;
    private final Instant createdAt;
    private CommandStatus status;

    private LearningCommand(Builder builder) {
        this.commandId = builder.commandId;
        this.learningGoal = builder.learningGoal;
        this.context = builder.context;
        this.createdAt = builder.createdAt;
        this.status = CommandStatus.PENDING;
    }

    public static LearningCommand create(String learningGoal) {
        return new Builder()
                .commandId("learning-" + System.currentTimeMillis())
                .learningGoal(learningGoal)
                .context(new LearningContext("BEGINNER", "READING", 0))
                .createdAt(Instant.now())
                .build();
    }

    @Override
    public String getCommandId() { return commandId; }

    @Override
    public CommandType getType() { return CommandType.LEARNING; }

    @Override
    public String getDescription() { return "Learning: " + learningGoal; }

    @Override
    public Instant getCreatedAt() { return createdAt; }

    @Override
    public CommandStatus getStatus() { return status; }

    public String getLearningGoal() { return learningGoal; }

    public LearningContext getContext() { return context; }

    public void setStatus(CommandStatus status) { this.status = status; }

    public static Builder builder() { return new Builder(); }

    /**
     * 学习上下文
     */
    public record LearningContext(
            String currentLevel,      // BEGINNER, INTERMEDIATE, ADVANCED
            String learningStyle,      // VISUAL, AUDITORY, READING, KINESTHETIC
            int availableTimeMinutes
    ) {
        public LearningContext {
            if (currentLevel == null) currentLevel = "BEGINNER";
            if (learningStyle == null) learningStyle = "READING";
        }
    }

    public static final class Builder {
        private String commandId = "";
        private String learningGoal = "";
        private LearningContext context = new LearningContext("BEGINNER", "READING", 0);
        private Instant createdAt = Instant.now();

        public Builder commandId(String commandId) { this.commandId = commandId; return this; }
        public Builder learningGoal(String learningGoal) { this.learningGoal = learningGoal; return this; }
        public Builder context(LearningContext context) { this.context = context; return this; }
        public Builder createdAt(Instant createdAt) { this.createdAt = createdAt; return this; }

        public LearningCommand build() { return new LearningCommand(this); }
    }
}
