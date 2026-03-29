package com.lingfeng.sprite.domain.command;

import java.time.Instant;

/**
 * ActionCommand - 操作命令
 *
 * 用户请求 Sprite 执行操作的命令。
 *
 * 对应 IGN-021
 */
public final class ActionCommand implements Command {

    private final String commandId;
    private final String actionDescription;
    private final ActionTarget target;
    private final Instant createdAt;
    private CommandStatus status;

    private ActionCommand(Builder builder) {
        this.commandId = builder.commandId;
        this.actionDescription = builder.actionDescription;
        this.target = builder.target;
        this.createdAt = builder.createdAt;
        this.status = CommandStatus.PENDING;
    }

    public static ActionCommand create(String actionDescription, ActionTarget target) {
        return new Builder()
                .commandId("action-" + System.currentTimeMillis())
                .actionDescription(actionDescription)
                .target(target)
                .createdAt(Instant.now())
                .build();
    }

    @Override
    public String getCommandId() { return commandId; }

    @Override
    public CommandType getType() { return CommandType.ACTION; }

    @Override
    public String getDescription() { return actionDescription; }

    @Override
    public Instant getCreatedAt() { return createdAt; }

    @Override
    public CommandStatus getStatus() { return status; }

    public String getActionDescription() { return actionDescription; }

    public ActionTarget getTarget() { return target; }

    public void setStatus(CommandStatus status) { this.status = status; }

    public static Builder builder() { return new Builder(); }

    /**
     * 操作目标
     */
    public record ActionTarget(
            String deviceId,
            String targetType,  // DEVICE, FILE, APPLICATION, SYSTEM
            String targetPath
    ) {}

    public static final class Builder {
        private String commandId = "";
        private String actionDescription = "";
        private ActionTarget target = new ActionTarget("", "", "");
        private Instant createdAt = Instant.now();

        public Builder commandId(String commandId) { this.commandId = commandId; return this; }
        public Builder actionDescription(String actionDescription) { this.actionDescription = actionDescription; return this; }
        public Builder target(ActionTarget target) { this.target = target; return this; }
        public Builder createdAt(Instant createdAt) { this.createdAt = createdAt; return this; }

        public ActionCommand build() { return new ActionCommand(this); }
    }
}
