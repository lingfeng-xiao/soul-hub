package com.lingfeng.sprite.agent;

import com.lingfeng.sprite.*;
import com.lingfeng.sprite.agent.config.AgentConfig;
import com.lingfeng.sprite.cognition.CognitionController;
import com.lingfeng.sprite.cognition.ReasoningEngine;
import com.lingfeng.sprite.mcp.McpClient;
import com.lingfeng.sprite.skill.SkillRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Leader Agent - Central orchestrator for the multi-agent sprite system.
 *
 * Maintains global SelfModel, WorldModel and routes tasks to appropriate workers.
 * This is a refactored version that provides multi-agent coordination on top of
 * the existing Sprite architecture.
 */
@Component
public class LeaderAgent {
    private static final Logger logger = LoggerFactory.getLogger(LeaderAgent.class);

    private final String leaderId;
    private final Mailbox mailbox;
    private final AgentRegistry registry;
    private final AgentConfig config;

    // Core sprite components (owned by leader)
    private SelfModel.Self selfModel;
    private WorldModel.World worldModel;
    private MemorySystem.Memory memory;
    private EvolutionEngine.Engine evolutionEngine;
    private ReasoningEngine reasoningEngine;
    private SkillRegistry skillRegistry;
    private McpClient mcpClient;

    // Workers
    private final List<WorkerAgent> allWorkers = new CopyOnWriteArrayList<>();
    private PerceptionWorker perceptionWorker;
    private CognitionWorker cognitionWorker;
    private ActionWorker actionWorker;

    // State
    private final AtomicBoolean running = new AtomicBoolean(false);
    private volatile Thread mainLoopThread;
    private ScheduledExecutorService scheduler;

    // Cognition cycle state
    private volatile Instant lastCycleTime;
    private volatile CognitionController.CognitionResult lastCognitionResult;

    public LeaderAgent(AgentConfig config, AgentRegistry registry, SkillRegistry skillRegistry) {
        this.config = config;
        this.leaderId = config.getLeader().getId();
        this.mailbox = new Mailbox(
            leaderId,
            config.getMailbox().getBasePath(),
            config.getMailbox().getPollIntervalMs()
        );
        this.registry = registry;
        this.skillRegistry = skillRegistry;
        this.mcpClient = new McpClient("sprite-agent");
        logger.info("LeaderAgent created with ID {}", leaderId);
    }

    /**
     * Initialize the sprite system with existing components
     */
    public void initialize(
            SelfModel.Self selfModel,
            WorldModel.World worldModel,
            MemorySystem.Memory memory,
            EvolutionEngine.Engine evolutionEngine,
            ReasoningEngine reasoningEngine,
            CognitionController cognitionController,
            PerceptionSystem.System perceptionSystem
    ) {
        this.selfModel = selfModel;
        this.worldModel = worldModel;
        this.memory = memory;
        this.evolutionEngine = evolutionEngine;
        this.reasoningEngine = reasoningEngine;

        // Initialize workers
        initializeWorkers(perceptionSystem, cognitionController);

        logger.info("LeaderAgent initialized with all components");
    }

    private void initializeWorkers(PerceptionSystem.System perceptionSystem, CognitionController cognitionController) {
        // Create specialized workers
        perceptionWorker = new PerceptionWorker(
            "perception-1",
            mailbox,
            registry,
            perceptionSystem,
            memory
        );

        cognitionWorker = new CognitionWorker(
            "cognition-1",
            mailbox,
            registry,
            cognitionController,
            selfModel,
            worldModel,
            memory,
            reasoningEngine
        );

        actionWorker = new ActionWorker(
            "action-1",
            mailbox,
            registry,
            memory,
            selfModel,
            worldModel
        );

        // Set up skill executor and MCP client for action worker
        SkillExecutor skillExecutor = new SkillExecutor(skillRegistry);
        actionWorker.setSkillExecutor(skillExecutor);
        actionWorker.setMcpClient(mcpClient);

        allWorkers.addAll(List.of(perceptionWorker, cognitionWorker, actionWorker));

        // Initialize all workers
        perceptionWorker.initialize();
        cognitionWorker.initialize();
        actionWorker.initialize();

        logger.info("All workers initialized: {}", allWorkers.size());
    }

    /**
     * Start the sprite system
     */
    public void start() {
        if (!running.compareAndSet(false, true)) {
            logger.warn("Sprite already running");
            return;
        }

        logger.info("Starting sprite system...");

        // Start all workers
        allWorkers.forEach(WorkerAgent::start);

        // Start mailbox polling for leader
        mailbox.startPolling(this::handleMessage);

        // Start scheduler for periodic cognition cycles
        scheduler = Executors.newScheduledThreadPool(1);
        long intervalMs = config.getIntervalMs();

        scheduler.scheduleAtFixedRate(() -> {
            if (running.get()) {
                triggerCognitionCycle();
            }
        }, 0, intervalMs, TimeUnit.MILLISECONDS);

        logger.info("Sprite system started");
    }

    /**
     * Stop the sprite system
     */
    public void stop() {
        if (!running.compareAndSet(true, false)) {
            logger.warn("Sprite not running");
            return;
        }

        logger.info("Stopping sprite system...");

        // Stop scheduler
        if (scheduler != null) {
            scheduler.shutdown();
            try {
                scheduler.awaitTermination(5, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                scheduler.shutdownNow();
            }
        }

        // Shutdown workers
        allWorkers.forEach(WorkerAgent::shutdown);

        // Stop mailbox polling
        mailbox.stopPolling();

        // Save memory
        if (memory != null) {
            memory.save();
        }

        logger.info("Sprite system stopped");
    }

    /**
     * Trigger a cognition cycle
     */
    public void triggerCognitionCycle() {
        lastCycleTime = Instant.now();

        // Send perception task to perception worker
        Task perceptionTask = Task.create("PERCEPTION", "Collect perception data", Map.of());
        perceptionWorker.submitTask(perceptionTask);

        // Send cognition task to cognition worker
        Task cognitionTask = Task.create("COGNITION", "Process cognition", Map.of(
            "perception", perceptionTask
        ));
        cognitionWorker.submitTask(cognitionTask);
    }

    /**
     * Record feedback for evolution
     */
    public void recordFeedback(EvolutionEngine.Feedback.FeedbackSource type, String content,
                                String outcome, boolean success, EvolutionEngine.Impact impact) {
        EvolutionEngine.Feedback feedback;
        switch (type) {
            case OWNER_EXPLICIT -> feedback = new EvolutionEngine.Feedback.OwnerFeedback(
                Instant.now(), content, 0.5f);
            case OUTCOME_SUCCESS -> feedback = new EvolutionEngine.Feedback.OutcomeFeedback(
                Instant.now(), content, success, outcome, impact);
            case OUTCOME_FAILURE -> feedback = new EvolutionEngine.Feedback.OutcomeFeedback(
                Instant.now(), content, success, outcome, impact);
            default -> feedback = new EvolutionEngine.Feedback.SelfReviewFeedback(
                Instant.now(), content, "", "");
        }
        if (evolutionEngine != null) {
            evolutionEngine.collectFeedback(feedback);
        }
    }

    /**
     * Trigger evolution
     */
    public EvolutionEngine.EvolutionResult evolve() {
        if (evolutionEngine == null || selfModel == null) {
            return null;
        }
        EvolutionEngine.EvolutionResult result = evolutionEngine.evolve(selfModel, worldModel);
        if (result != null && result.success() && result.updatedSelf() != null) {
            selfModel = result.updatedSelf();
        }
        return result;
    }

    /**
     * Handle incoming messages from workers
     */
    private void handleMessage(MailboxMessage msg) {
        logger.debug("Leader received message type {} from {}", msg.type(), msg.from());
        switch (msg.type()) {
            case RESULT -> {
                if (msg.payload() instanceof Task task) {
                    handleTaskResult(task);
                }
            }
            case HEARTBEAT -> {
                logger.trace("Heartbeat from {}", msg.from());
            }
            case ERROR -> {
                logger.error("Error from {}: {}", msg.from(), msg.payload());
            }
            case REGISTER -> {
                logger.info("Worker {} registered", msg.from());
            }
            case DEREGISTER -> {
                logger.info("Worker {} deregistered", msg.from());
            }
            default -> logger.debug("Unhandled message type {} from {}", msg.type(), msg.from());
        }
    }

    private void handleTaskResult(Task task) {
        logger.debug("Task result received: {} ({})", task.taskId(), task.type());
        if ("COGNITION".equals(task.type()) && task.result() instanceof CognitionController.CognitionResult result) {
            lastCognitionResult = result;
        }
    }

    // Getters for state

    public boolean isRunning() {
        return running.get();
    }

    public Instant getLastCycleTime() {
        return lastCycleTime;
    }

    public CognitionController.CognitionResult getLastCognitionResult() {
        return lastCognitionResult;
    }

    public SelfModel.Self getSelfModel() {
        return selfModel;
    }

    public WorldModel.World getWorldModel() {
        return worldModel;
    }

    public MemorySystem.Memory getMemory() {
        return memory;
    }

    public AgentRegistry getRegistry() {
        return registry;
    }

    public String getLeaderId() {
        return leaderId;
    }

    public List<WorkerAgent> getAllWorkers() {
        return new ArrayList<>(allWorkers);
    }
}
