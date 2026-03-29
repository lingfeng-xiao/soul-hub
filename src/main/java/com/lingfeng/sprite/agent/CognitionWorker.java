package com.lingfeng.sprite.agent;

import com.lingfeng.sprite.MemorySystem;
import com.lingfeng.sprite.SelfModel;
import com.lingfeng.sprite.WorldModel;
import com.lingfeng.sprite.cognition.CognitionController;
import com.lingfeng.sprite.cognition.ReasoningEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * Cognition Worker - handles reasoning and decision making
 */
public class CognitionWorker extends WorkerAgent {
    private static final Logger logger = LoggerFactory.getLogger(CognitionWorker.class);

    private final CognitionController cognitionController;
    private volatile SelfModel.Self selfModel;
    private volatile WorldModel.World worldModel;
    private final MemorySystem.Memory memory;
    private final ReasoningEngine reasoningEngine;

    public CognitionWorker(String workerId, Mailbox mailbox, AgentRegistry registry,
                           CognitionController cognitionController,
                           SelfModel.Self selfModel, WorldModel.World worldModel,
                           MemorySystem.Memory memory, ReasoningEngine reasoningEngine) {
        super(workerId, WorkerType.COGNITION, mailbox, registry);
        this.cognitionController = cognitionController;
        this.selfModel = selfModel;
        this.worldModel = worldModel;
        this.memory = memory;
        this.reasoningEngine = reasoningEngine;
    }

    @Override
    protected void doInitialize() {
        logger.info("CognitionWorker {} initialized", workerId);
    }

    @Override
    protected Task doProcessTask(Task task) {
        logger.debug("CognitionWorker {} processing {} task", workerId, task.type());

        if ("COGNITION".equals(task.type())) {
            return runCognition(task);
        } else if ("UPDATE_WORLD".equals(task.type())) {
            return updateWorldModel(task);
        } else if ("UPDATE_SELF".equals(task.type())) {
            return updateSelfModel(task);
        }

        return task.withError("Unknown task type: " + task.type());
    }

    private Task runCognition(Task task) {
        try {
            CognitionController.CognitionResult result = cognitionController.cognitionCycle();

            // Update models if changed
            if (result != null) {
                if (result.selfModel() != null) {
                    this.selfModel = result.selfModel();
                }
                if (result.worldModel() != null) {
                    this.worldModel = result.worldModel();
                }
            }

            String reflectionSummary = "";
            if (result != null && result.reflection() != null) {
                reflectionSummary = result.reflection().toString();
            }

            return task.withResult(Map.of(
                "cognitionCompleted", result != null,
                "reflection", reflectionSummary,
                "hasActionRecommendation", result != null && result.actionRecommendation() != null
            ));
        } catch (Exception e) {
            logger.error("CognitionWorker {} failed to run cognition", workerId, e);
            return task.withError(e.getMessage());
        }
    }

    private Task updateWorldModel(Task task) {
        try {
            Object update = task.parameters().get("update");
            logger.debug("Updating world model with: {}", update);
            return task.withResult(Map.of("worldModelUpdated", true));
        } catch (Exception e) {
            return task.withError(e.getMessage());
        }
    }

    private Task updateSelfModel(Task task) {
        try {
            Object update = task.parameters().get("update");
            logger.debug("Updating self model with: {}", update);
            return task.withResult(Map.of("selfModelUpdated", true));
        } catch (Exception e) {
            return task.withError(e.getMessage());
        }
    }
}
