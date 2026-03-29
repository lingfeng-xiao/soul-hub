package com.lingfeng.sprite.life;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lingfeng.sprite.controller.dto.LifeCommandRequest;
import com.lingfeng.sprite.controller.dto.LifeCommandResponse;
import com.lingfeng.sprite.controller.dto.LifeCommandResultDto;
import com.lingfeng.sprite.domain.command.ActionCommand;
import com.lingfeng.sprite.domain.command.AskCommand;
import com.lingfeng.sprite.domain.command.Command;
import com.lingfeng.sprite.domain.command.CommandOrchestrator;
import com.lingfeng.sprite.domain.command.CommandResult;
import com.lingfeng.sprite.domain.command.DecisionCommand;
import com.lingfeng.sprite.domain.command.ImpactReport;
import com.lingfeng.sprite.domain.command.LearningCommand;
import com.lingfeng.sprite.domain.command.ResearchCommand;
import com.lingfeng.sprite.domain.command.TaskCommand;
import com.lingfeng.sprite.domain.goal.ActiveIntention;
import com.lingfeng.sprite.domain.goal.GoalService;
import com.lingfeng.sprite.domain.relationship.RelationshipService;
import com.lingfeng.sprite.domain.self.AttentionFocus;
import com.lingfeng.sprite.domain.self.SelfService;
import com.lingfeng.sprite.domain.snapshot.LifeSnapshotService;
import com.lingfeng.sprite.life.persistence.LifeCommandExecutionEntity;
import com.lingfeng.sprite.life.persistence.LifeCommandExecutionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Service
public class LifeCommandService {

    private static final Logger logger = LoggerFactory.getLogger(LifeCommandService.class);

    private final CommandOrchestrator orchestrator;
    private final SelfService selfService;
    private final RelationshipService relationshipService;
    private final GoalService goalService;
    private final LifeSnapshotService lifeSnapshotService;
    private final LifeJournalService lifeJournalService;
    private final LifeRuntimeStateService lifeRuntimeStateService;
    private final LifeCommandExecutionRepository executionRepository;
    private final ObjectMapper objectMapper;

    public LifeCommandService(
            CommandOrchestrator orchestrator,
            SelfService selfService,
            RelationshipService relationshipService,
            GoalService goalService,
            LifeSnapshotService lifeSnapshotService,
            LifeJournalService lifeJournalService,
            LifeRuntimeStateService lifeRuntimeStateService,
            LifeCommandExecutionRepository executionRepository,
            ObjectMapper objectMapper
    ) {
        this.orchestrator = orchestrator;
        this.selfService = selfService;
        this.relationshipService = relationshipService;
        this.goalService = goalService;
        this.lifeSnapshotService = lifeSnapshotService;
        this.lifeJournalService = lifeJournalService;
        this.lifeRuntimeStateService = lifeRuntimeStateService;
        this.executionRepository = executionRepository;
        this.objectMapper = objectMapper;
    }

    public LifeCommandResponse execute(LifeCommandRequest request) {
        if (!isSupportedType(request.type())) {
            Command rejectedCommand = AskCommand.create(request.content());
            CommandResult rejected = reject(rejectedCommand, "Unsupported command type: " + request.type());
            persistExecution(request, rejectedCommand, rejected);
            lifeJournalService.record(
                    "COMMAND",
                    "Rejected command",
                    rejected.getDetail(),
                    Map.of("commandId", rejectedCommand.getCommandId(), "commandType", request.type())
            );
            return new LifeCommandResponse(
                    LifeCommandResultDto.from(request.type(), rejected),
                    rejected.getImpact(),
                    lifeSnapshotService.generateSnapshot()
            );
        }

        Command command = buildCommand(request);
        if (request.content() == null || request.content().isBlank()) {
            CommandResult rejected = reject(command, "Command content cannot be empty.");
            persistExecution(request, command, rejected);
            lifeJournalService.record(
                    "COMMAND",
                    "Rejected command",
                    rejected.getDetail(),
                    Map.of("commandId", command.getCommandId(), "commandType", request.type())
            );
            return new LifeCommandResponse(
                    LifeCommandResultDto.from(request.type(), rejected),
                    rejected.getImpact(),
                    lifeSnapshotService.generateSnapshot()
            );
        }

        prepareStateForCommand(request, command);

        CommandResult result;
        try {
            result = orchestrator.execute(command);
        } catch (RuntimeException exception) {
            result = reject(command, "Command execution failed: " + exception.getMessage());
        }
        applyImpact(request, command, result);
        persistExecution(request, command, result);
        lifeJournalService.recordCommand(request, result);
        lifeRuntimeStateService.persistCurrentState();

        return new LifeCommandResponse(
                LifeCommandResultDto.from(request.type(), result),
                result.getImpact(),
                lifeSnapshotService.generateSnapshot()
        );
    }

    private Command buildCommand(LifeCommandRequest request) {
        return switch (request.type()) {
            case "TASK" -> TaskCommand.create(request.content());
            case "RESEARCH" -> ResearchCommand.create(request.content());
            case "ACTION" -> ActionCommand.create(
                    request.content(),
                    new ActionCommand.ActionTarget(
                            asString(request.context().get("deviceId")),
                            asString(request.context().get("targetType")),
                            asString(request.context().get("targetPath"))
                    )
            );
            case "LEARNING" -> LearningCommand.create(request.content());
            case "DECISION" -> DecisionCommand.create(request.content(), asStringList(request.context().get("options")));
            case "ASK" -> AskCommand.create(request.content());
            default -> throw new IllegalArgumentException("Unsupported command type: " + request.type());
        };
    }

    private void prepareStateForCommand(LifeCommandRequest request, Command command) {
        AttentionFocus focus = AttentionFocus.builder()
                .type(resolveFocusType(request.type()))
                .description(request.content())
                .relatedEntityId(command.getCommandId())
                .intensity(0.9f)
                .startedAt(Instant.now())
                .expectedDurationMs(900000)
                .build();
        selfService.updateFocus(focus);
        relationshipService.recordInteraction();
    }

    private void applyImpact(LifeCommandRequest request, Command command, CommandResult result) {
        ImpactReport impact = result.getImpact();

        if (impact.getSelfUpdate().energyChanged()) {
            float delta = impact.getSelfUpdate().energyDelta();
            if (delta < 0) {
                selfService.drainEnergy(Math.abs(delta));
            } else {
                selfService.restoreEnergy(delta);
            }
        }

        if (impact.getSelfUpdate().focusChanged()) {
            selfService.updateFocus(AttentionFocus.builder()
                    .type(resolveFocusType(request.type()))
                    .description(nonBlank(impact.getSelfUpdate().newFocus(), request.content()))
                    .relatedEntityId(command.getCommandId())
                    .intensity(0.8f)
                    .startedAt(Instant.now())
                    .expectedDurationMs(900000)
                    .build());
        }

        if (impact.getRelationshipUpdate().trustChanged()) {
            float trustDelta = impact.getRelationshipUpdate().trustDelta();
            if (trustDelta >= 0) {
                relationshipService.increaseTrust(trustDelta);
            } else {
                relationshipService.decreaseTrust(Math.abs(trustDelta));
            }
        }

        if (shouldCreateIntention(request, impact)) {
            goalService.createIntention(command.getCommandId(), request.content(), urgencyFor(request.type()));
        }

        selfService.updateCoherence(result.isSuccess() ? 0.02f : -0.05f);
        lifeJournalService.record(
                "SELF",
                "Focus updated",
                "Current focus: " + selfService.getCurrentFocus().getDescription(),
                Map.of("commandId", command.getCommandId())
        );
    }

    private void persistExecution(LifeCommandRequest request, Command command, CommandResult result) {
        try {
            LifeCommandExecutionEntity entity = new LifeCommandExecutionEntity();
            entity.setCommandId(command.getCommandId());
            entity.setCommandType(request.type());
            entity.setContent(request.content());
            entity.setContextJson(objectMapper.writeValueAsString(request.context()));
            entity.setSource(request.source());
            entity.setSummary(result.getSummary());
            entity.setDetail(result.getDetail());
            entity.setSuccess(result.isSuccess());
            entity.setImpactJson(objectMapper.writeValueAsString(result.getImpact()));
            entity.setCreatedAt(Instant.now());
            executionRepository.save(entity);
        } catch (Exception exception) {
            logger.debug("Failed to persist command execution: {}", exception.getMessage());
        }
    }

    private boolean shouldCreateIntention(LifeCommandRequest request, ImpactReport impact) {
        if (impact.getGoalUpdate().intentionTriggered()) {
            return true;
        }
        return switch (request.type()) {
            case "TASK", "RESEARCH", "LEARNING", "ACTION", "DECISION" -> true;
            default -> false;
        };
    }

    private ActiveIntention.Urgency urgencyFor(String type) {
        return switch (type) {
            case "ACTION", "TASK" -> ActiveIntention.Urgency.HIGH;
            case "DECISION" -> ActiveIntention.Urgency.NORMAL;
            case "RESEARCH", "LEARNING" -> ActiveIntention.Urgency.NORMAL;
            default -> ActiveIntention.Urgency.LOW;
        };
    }

    private AttentionFocus.FocusType resolveFocusType(String type) {
        return switch (type) {
            case "TASK", "ACTION" -> AttentionFocus.FocusType.TASK;
            case "LEARNING" -> AttentionFocus.FocusType.LEARNING;
            case "RESEARCH", "DECISION", "ASK" -> AttentionFocus.FocusType.CONVERSATION;
            default -> AttentionFocus.FocusType.CONVERSATION;
        };
    }

    private List<String> asStringList(Object value) {
        if (value instanceof List<?> list) {
            return list.stream().map(String::valueOf).toList();
        }
        return List.of("Keep the current direction", "Change approach", "Wait for more signal");
    }

    private String asString(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private String nonBlank(String preferred, String fallback) {
        return preferred != null && !preferred.isBlank() ? preferred : fallback;
    }

    private boolean isSupportedType(String type) {
        return switch (type) {
            case "TASK", "RESEARCH", "ACTION", "LEARNING", "DECISION", "ASK" -> true;
            default -> false;
        };
    }

    private CommandResult reject(Command command, String reason) {
        if (command != null) {
            markRejected(command);
        }
        return new RejectedCommandResult(
                command != null ? command.getCommandId() : "rejected-" + Instant.now().toEpochMilli(),
                reason,
                ImpactReport.none()
        );
    }

    private void markRejected(Command command) {
        if (command instanceof AskCommand askCommand) {
            askCommand.setStatus(Command.CommandStatus.FAILED);
        } else if (command instanceof TaskCommand taskCommand) {
            taskCommand.setStatus(Command.CommandStatus.FAILED);
        } else if (command instanceof ResearchCommand researchCommand) {
            researchCommand.setStatus(Command.CommandStatus.FAILED);
        } else if (command instanceof ActionCommand actionCommand) {
            actionCommand.setStatus(Command.CommandStatus.FAILED);
        } else if (command instanceof LearningCommand learningCommand) {
            learningCommand.setStatus(Command.CommandStatus.FAILED);
        } else if (command instanceof DecisionCommand decisionCommand) {
            decisionCommand.setStatus(Command.CommandStatus.FAILED);
        } else {
            logger.debug("Command type {} does not expose mutable status", command.getClass().getName());
        }
    }

    private record RejectedCommandResult(
            String commandId,
            String detail,
            ImpactReport impact
    ) implements CommandResult {
        @Override
        public String getCommandId() {
            return commandId;
        }

        @Override
        public boolean isSuccess() {
            return false;
        }

        @Override
        public String getSummary() {
            return detail.length() <= 72 ? detail : detail.substring(0, 69) + "...";
        }

        @Override
        public String getDetail() {
            return detail;
        }

        @Override
        public ImpactReport getImpact() {
            return impact;
        }
    }
}
