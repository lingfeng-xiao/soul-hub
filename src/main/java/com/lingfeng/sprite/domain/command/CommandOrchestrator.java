package com.lingfeng.sprite.domain.command;

import com.lingfeng.sprite.domain.pacing.EvolutionPacingEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * CommandOrchestrator - command execution coordinator.
 */
@Service
public final class CommandOrchestrator {

    private static final Logger logger = LoggerFactory.getLogger(CommandOrchestrator.class);

    private final Map<String, Command> commandRegistry;
    private final List<CommandResult> resultHistory;
    private EvolutionPacingEngine pacingEngine;

    public CommandOrchestrator() {
        this.commandRegistry = new ConcurrentHashMap<>();
        this.resultHistory = Collections.synchronizedList(new ArrayList<>());
    }

    public void setPacingEngine(EvolutionPacingEngine pacingEngine) {
        this.pacingEngine = pacingEngine;
    }

    public void register(Command command) {
        commandRegistry.put(command.getCommandId(), command);
        logger.debug("Command registered: {}", command.getCommandId());
    }

    public CommandResult execute(Command command) {
        logger.info("Executing command: {} ({})", command.getCommandId(), command.getType());

        register(command);

        CommandResult result = switch (command.getType()) {
            case ASK -> executeAsk((AskCommand) command);
            case TASK -> executeTask((TaskCommand) command);
            case RESEARCH -> executeResearch((ResearchCommand) command);
            case DECISION -> executeDecision((DecisionCommand) command);
            case LEARNING -> executeLearning((LearningCommand) command);
            case ACTION -> executeAction((ActionCommand) command);
        };

        resultHistory.add(result);
        return result;
    }

    private CommandResult executeAsk(AskCommand command) {
        command.setStatus(Command.CommandStatus.EXECUTING);

        String question = normalize(command.getQuestion());
        String answer = question.isBlank()
                ? "Question captured for later reflection."
                : "Question routed through the life loop: " + question;
        String detail = question.isBlank()
                ? "The question was empty, so it was retained as a prompt instead of being answered."
                : "The question was recorded and linked to the current life loop state for later follow-up.";

        command.setStatus(Command.CommandStatus.COMPLETED);

        ImpactReport impact = ImpactReport.builder()
                .selfUpdate(new ImpactReport.SelfUpdate(false, 0, false, "", ""))
                .relationshipUpdate(new ImpactReport.RelationshipUpdate(true, false, 0.01f, "CONVERSATION"))
                .memoryUpdate(new ImpactReport.MemoryUpdate(true, "EPISODIC", command.getCommandId()))
                .goalUpdate(ImpactReport.GoalUpdate.NONE)
                .growthUpdate(ImpactReport.GrowthUpdate.NONE)
                .build();

        return new AskResult(command.getCommandId(), answer, null, impact, true, detail);
    }

    private CommandResult executeTask(TaskCommand command) {
        command.setStatus(Command.CommandStatus.EXECUTING);

        String task = normalize(command.getTaskDescription());
        String output = task.isBlank()
                ? "Task registered without a description."
                : "Task staged for execution: " + task;
        String detail = "Priority=" + command.getContext().priority()
                + ", deadline=" + normalize(command.getContext().deadline())
                + ", metadataKeys=" + command.getContext().metadata().size();

        command.setStatus(Command.CommandStatus.COMPLETED);

        ImpactReport impact = ImpactReport.builder()
                .selfUpdate(new ImpactReport.SelfUpdate(true, -0.1f, true, "task_execution", detail))
                .relationshipUpdate(new ImpactReport.RelationshipUpdate(true, false, 0.02f, "TASK_COLLABORATION"))
                .memoryUpdate(new ImpactReport.MemoryUpdate(true, "EPISODIC", command.getCommandId()))
                .goalUpdate(new ImpactReport.GoalUpdate(true, command.getCommandId(), true, "goal-1", 0.05f))
                .growthUpdate(ImpactReport.GrowthUpdate.NONE)
                .build();

        return new TaskResult(command.getCommandId(), output, impact, true, detail);
    }

    private CommandResult executeResearch(ResearchCommand command) {
        command.setStatus(Command.CommandStatus.EXECUTING);

        String topic = normalize(command.getTopic());
        String output = topic.isBlank()
                ? "Research scope registered without a topic."
                : "Research scope captured: " + topic;
        String detail = "Scope=" + command.getScope().name() + ", topic=" + topic;

        command.setStatus(Command.CommandStatus.COMPLETED);

        ImpactReport impact = ImpactReport.builder()
                .selfUpdate(new ImpactReport.SelfUpdate(false, 0, false, "research", detail))
                .memoryUpdate(new ImpactReport.MemoryUpdate(true, "SEMANTIC", command.getCommandId()))
                .growthUpdate(new ImpactReport.GrowthUpdate(true, "INSIGHT_GAINED", "Research scope expanded for " + topic, false, ""))
                .build();

        return new ResearchResult(command.getCommandId(), output, impact, true, detail);
    }

    private CommandResult executeDecision(DecisionCommand command) {
        command.setStatus(Command.CommandStatus.EXECUTING);

        String recommendation = command.getOptions().isEmpty()
                ? "Decision support captured without options."
                : "Recommended option: " + command.getOptions().get(0);
        String detail = "Options=" + command.getOptions().size()
                + ", context=" + normalize(command.getDecisionContext());

        command.setStatus(Command.CommandStatus.COMPLETED);

        ImpactReport impact = ImpactReport.builder()
                .selfUpdate(new ImpactReport.SelfUpdate(false, 0, true, "decision_support", detail))
                .memoryUpdate(new ImpactReport.MemoryUpdate(true, "EPISODIC", command.getCommandId()))
                .build();

        return new DecisionResult(command.getCommandId(), recommendation, command.getOptions(), impact, true, detail);
    }

    private CommandResult executeLearning(LearningCommand command) {
        command.setStatus(Command.CommandStatus.EXECUTING);

        String goal = normalize(command.getLearningGoal());
        String plan = goal.isBlank()
                ? "Learning goal captured without detail."
                : "Learning plan staged for: " + goal;
        String detail = "Level=" + command.getContext().currentLevel()
                + ", style=" + command.getContext().learningStyle()
                + ", availableMinutes=" + command.getContext().availableTimeMinutes();

        command.setStatus(Command.CommandStatus.COMPLETED);

        ImpactReport impact = ImpactReport.builder()
                .goalUpdate(new ImpactReport.GoalUpdate(true, command.getCommandId(), false, "", 0.1f))
                .growthUpdate(new ImpactReport.GrowthUpdate(true, "SKILL_ACQUIRED", "Learning goal staged: " + goal, false, ""))
                .build();

        return new LearningResult(command.getCommandId(), plan, impact, true, detail);
    }

    private CommandResult executeAction(ActionCommand command) {
        command.setStatus(Command.CommandStatus.EXECUTING);

        String description = normalize(command.getActionDescription());
        String result = description.isBlank()
                ? "Action registered without a description."
                : "Action staged: " + description;
        String detail = "targetType=" + normalize(command.getTarget().targetType())
                + ", targetPath=" + normalize(command.getTarget().targetPath())
                + ", deviceId=" + normalize(command.getTarget().deviceId());

        command.setStatus(Command.CommandStatus.COMPLETED);

        ImpactReport impact = ImpactReport.builder()
                .selfUpdate(new ImpactReport.SelfUpdate(true, -0.15f, true, "action_execution", detail))
                .memoryUpdate(new ImpactReport.MemoryUpdate(true, "PROCEDURAL", command.getCommandId()))
                .build();

        return new ActionResult(command.getCommandId(), result, impact, true, detail);
    }

    public List<CommandResult> getHistory(int limit) {
        int size = resultHistory.size();
        int from = Math.max(0, size - limit);
        return new ArrayList<>(resultHistory.subList(from, size));
    }

    public List<Command> getPendingCommands() {
        return commandRegistry.values().stream()
                .filter(c -> c.getStatus() == Command.CommandStatus.PENDING
                        || c.getStatus() == Command.CommandStatus.EXECUTING)
                .toList();
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }

    public record AskResult(
            String commandId,
            String answer,
            String sources,
            ImpactReport impact,
            boolean success,
            String detail
    ) implements CommandResult {
        @Override public String getCommandId() { return commandId; }
        @Override public boolean isSuccess() { return success; }
        @Override public String getSummary() { return answer.substring(0, Math.min(50, answer.length())); }
        @Override public String getDetail() { return detail; }
        @Override public ImpactReport getImpact() { return impact; }
    }

    public record TaskResult(
            String commandId,
            String output,
            ImpactReport impact,
            boolean success,
            String detail
    ) implements CommandResult {
        @Override public String getCommandId() { return commandId; }
        @Override public boolean isSuccess() { return success; }
        @Override public String getSummary() { return output.substring(0, Math.min(50, output.length())); }
        @Override public String getDetail() { return detail; }
        @Override public ImpactReport getImpact() { return impact; }
    }

    public record ResearchResult(
            String commandId,
            String findings,
            ImpactReport impact,
            boolean success,
            String detail
    ) implements CommandResult {
        @Override public String getCommandId() { return commandId; }
        @Override public boolean isSuccess() { return success; }
        @Override public String getSummary() { return findings.substring(0, Math.min(50, findings.length())); }
        @Override public String getDetail() { return detail; }
        @Override public ImpactReport getImpact() { return impact; }
    }

    public record DecisionResult(
            String commandId,
            String recommendation,
            java.util.List<String> options,
            ImpactReport impact,
            boolean success,
            String detail
    ) implements CommandResult {
        @Override public String getCommandId() { return commandId; }
        @Override public boolean isSuccess() { return success; }
        @Override public String getSummary() { return recommendation; }
        @Override public String getDetail() { return detail; }
        @Override public ImpactReport getImpact() { return impact; }
    }

    public record LearningResult(
            String commandId,
            String plan,
            ImpactReport impact,
            boolean success,
            String detail
    ) implements CommandResult {
        @Override public String getCommandId() { return commandId; }
        @Override public boolean isSuccess() { return success; }
        @Override public String getSummary() { return plan.substring(0, Math.min(50, plan.length())); }
        @Override public String getDetail() { return detail; }
        @Override public ImpactReport getImpact() { return impact; }
    }

    public record ActionResult(
            String commandId,
            String result,
            ImpactReport impact,
            boolean success,
            String detail
    ) implements CommandResult {
        @Override public String getCommandId() { return commandId; }
        @Override public boolean isSuccess() { return success; }
        @Override public String getSummary() { return result.substring(0, Math.min(50, result.length())); }
        @Override public String getDetail() { return detail; }
        @Override public ImpactReport getImpact() { return impact; }
    }
}
