package com.lingfeng.sprite.domain.command;

import java.time.Instant;
import java.util.List;

/**
 * DecisionCommand - 决策命令
 *
 * 用户请求 Sprite 提供决策支持的命令。
 *
 * 对应 IGN-021
 */
public final class DecisionCommand implements Command {

    private final String commandId;
    private final String decisionContext;
    private final List<String> options;
    private final DecisionCriteria criteria;
    private final Instant createdAt;
    private CommandStatus status;

    private DecisionCommand(Builder builder) {
        this.commandId = builder.commandId;
        this.decisionContext = builder.decisionContext;
        this.options = builder.options;
        this.criteria = builder.criteria;
        this.createdAt = builder.createdAt;
        this.status = CommandStatus.PENDING;
    }

    public static DecisionCommand create(String context, List<String> options) {
        return new Builder()
                .commandId("decision-" + System.currentTimeMillis())
                .decisionContext(context)
                .options(options)
                .criteria(new DecisionCriteria("", List.of()))
                .createdAt(Instant.now())
                .build();
    }

    @Override
    public String getCommandId() { return commandId; }

    @Override
    public CommandType getType() { return CommandType.DECISION; }

    @Override
    public String getDescription() { return "Decision: " + decisionContext; }

    @Override
    public Instant getCreatedAt() { return createdAt; }

    @Override
    public CommandStatus getStatus() { return status; }

    public String getDecisionContext() { return decisionContext; }

    public List<String> getOptions() { return options; }

    public DecisionCriteria getCriteria() { return criteria; }

    public void setStatus(CommandStatus status) { this.status = status; }

    public static Builder builder() { return new Builder(); }

    /**
     * 决策标准
     */
    public record DecisionCriteria(
            String primaryFactor,
            List<String> secondaryFactors
    ) {
        public DecisionCriteria {
            if (primaryFactor == null) primaryFactor = "";
            if (secondaryFactors == null) secondaryFactors = List.of();
        }
    }

    public static final class Builder {
        private String commandId = "";
        private String decisionContext = "";
        private List<String> options = List.of();
        private DecisionCriteria criteria = new DecisionCriteria("", List.of());
        private Instant createdAt = Instant.now();

        public Builder commandId(String commandId) { this.commandId = commandId; return this; }
        public Builder decisionContext(String decisionContext) { this.decisionContext = decisionContext; return this; }
        public Builder options(List<String> options) { this.options = options; return this; }
        public Builder criteria(DecisionCriteria criteria) { this.criteria = criteria; return this; }
        public Builder createdAt(Instant createdAt) { this.createdAt = createdAt; return this; }

        public DecisionCommand build() { return new DecisionCommand(this); }
    }
}
