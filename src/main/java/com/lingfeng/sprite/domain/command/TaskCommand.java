package com.lingfeng.sprite.domain.command;

import java.time.Instant;
import java.util.Map;

/**
 * TaskCommand - 任务命令
 *
 * 用户向 Sprite 发送任务请求的命令。
 *
 * 对应 IGN-021
 */
public final class TaskCommand implements Command {

    private final String commandId;
    private final String taskDescription;
    private final TaskContext context;
    private final Instant createdAt;
    private CommandStatus status;

    private TaskCommand(Builder builder) {
        this.commandId = builder.commandId;
        this.taskDescription = builder.taskDescription;
        this.context = builder.context;
        this.createdAt = builder.createdAt;
        this.status = CommandStatus.PENDING;
    }

    public static TaskCommand create(String taskDescription) {
        return new Builder()
                .commandId("task-" + System.currentTimeMillis())
                .taskDescription(taskDescription)
                .context(new TaskContext("NORMAL", "", Map.of()))
                .createdAt(Instant.now())
                .build();
    }

    @Override
    public String getCommandId() { return commandId; }

    @Override
    public CommandType getType() { return CommandType.TASK; }

    @Override
    public String getDescription() { return taskDescription; }

    @Override
    public Instant getCreatedAt() { return createdAt; }

    @Override
    public CommandStatus getStatus() { return status; }

    public String getTaskDescription() { return taskDescription; }

    public TaskContext getContext() { return context; }

    public void setStatus(CommandStatus status) { this.status = status; }

    public static Builder builder() { return new Builder(); }

    /**
     * 任务上下文
     */
    public record TaskContext(
            String priority,        // HIGH, NORMAL, LOW
            String deadline,
            Map<String, String> metadata
    ) {
        public TaskContext {
            if (priority == null) priority = "NORMAL";
            if (deadline == null) deadline = "";
            if (metadata == null) metadata = Map.of();
        }
    }

    public static final class Builder {
        private String commandId = "";
        private String taskDescription = "";
        private TaskContext context = new TaskContext("NORMAL", "", Map.of());
        private Instant createdAt = Instant.now();

        public Builder commandId(String commandId) { this.commandId = commandId; return this; }
        public Builder taskDescription(String taskDescription) { this.taskDescription = taskDescription; return this; }
        public Builder context(TaskContext context) { this.context = context; return this; }
        public Builder createdAt(Instant createdAt) { this.createdAt = createdAt; return this; }

        public TaskCommand build() { return new TaskCommand(this); }
    }
}
