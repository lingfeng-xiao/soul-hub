package com.lingfeng.sprite.agent;

import com.lingfeng.sprite.MemorySystem;
import com.lingfeng.sprite.PerceptionSystem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * Perception Worker - handles sensor data collection
 */
public class PerceptionWorker extends WorkerAgent {
    private static final Logger logger = LoggerFactory.getLogger(PerceptionWorker.class);

    private final PerceptionSystem.System perceptionSystem;
    private final MemorySystem.Memory memory;

    public PerceptionWorker(String workerId, Mailbox mailbox, AgentRegistry registry,
                           PerceptionSystem.System perceptionSystem, MemorySystem.Memory memory) {
        super(workerId, WorkerType.PERCEPTION, mailbox, registry);
        this.perceptionSystem = perceptionSystem;
        this.memory = memory;
    }

    @Override
    protected void doInitialize() {
        logger.info("PerceptionWorker {} initialized", workerId);
    }

    @Override
    protected Task doProcessTask(Task task) {
        logger.debug("PerceptionWorker {} processing {} task", workerId, task.type());

        if ("PERCEPTION".equals(task.type())) {
            return collectPerception(task);
        }

        return task.withError("Unknown task type: " + task.type());
    }

    private Task collectPerception(Task task) {
        try {
            // Run perception cycle using PerceptionSystem.System
            PerceptionSystem.PerceptionResult result = perceptionSystem.perceive();

            // Return result with perception data
            return task.withResult(Map.of(
                "perceptionTimestamp", result != null && result.perception() != null ?
                    result.perception().timestamp().toString() : null,
                "hasPlatformPerception", result != null && result.perception() != null &&
                    result.perception().platform() != null,
                "hasUserPerception", result != null && result.perception() != null &&
                    result.perception().user() != null,
                "hasEnvironmentPerception", result != null && result.perception() != null &&
                    result.perception().environment() != null,
                "salienceScore", result != null && result.attentionItem() != null ?
                    result.attentionItem().salience().overall() : 0.0
            ));
        } catch (Exception e) {
            logger.error("PerceptionWorker {} failed to collect perception", workerId, e);
            return task.withError(e.getMessage());
        }
    }
}
