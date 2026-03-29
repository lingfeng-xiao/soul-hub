package com.lingfeng.sprite.agent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Base worker agent class for thread-isolated multi-agent architecture.
 * Each worker runs in its own thread, communicating via Mailbox.
 */
public abstract class WorkerAgent {
    private static final Logger logger = LoggerFactory.getLogger(WorkerAgent.class);

    protected final String workerId;
    protected final WorkerType type;
    protected final Mailbox mailbox;
    protected final AgentRegistry registry;

    private final AtomicReference<WorkerState> state = new AtomicReference<>(WorkerState.CREATED);
    private volatile Thread runningThread;
    private volatile boolean shuttingDown = false;

    protected ExecutorService executor;
    protected BlockingQueue<Task> taskQueue;

    public WorkerAgent(String workerId, WorkerType type, Mailbox mailbox, AgentRegistry registry) {
        this.workerId = workerId;
        this.type = type;
        this.mailbox = mailbox;
        this.registry = registry;
        this.taskQueue = new PriorityBlockingQueue<>();
    }

    /**
     * Initialize the worker
     */
    public void initialize() {
        if (!state.compareAndSet(WorkerState.CREATED, WorkerState.INITIALIZING)) {
            throw new IllegalStateException("Worker already initialized");
        }
        executor = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "worker-" + type.getName() + "-" + workerId);
            t.setDaemon(true);
            return t;
        });
        state.set(WorkerState.INITIALIZING);
        logger.info("Worker {} ({}) initializing", workerId, type);
        doInitialize();
        registry.register(workerId, type);
        state.set(WorkerState.REGISTERED);
        logger.info("Worker {} ({}) initialized", workerId, type);
    }

    /**
     * Start the worker main loop
     */
    public void start() {
        if (state.get() != WorkerState.REGISTERED) {
            throw new IllegalStateException("Worker not in REGISTERED state");
        }
        runningThread = new Thread(() -> {
            logger.info("Worker {} ({}) starting main loop", workerId, type);
            registry.markRunning(workerId);
            state.set(WorkerState.RUNNING);

            // Register heartbeat
            ScheduledExecutorService heartbeat = Executors.newSingleThreadScheduledExecutor();
            heartbeat.scheduleAtFixedRate(() -> {
                registry.heartbeat(workerId);
                mailbox.sendTo("leader", MailboxMessage.MessageType.HEARTBEAT, workerId);
            }, 0, 5, TimeUnit.SECONDS);

            // Main loop
            while (!shuttingDown) {
                try {
                    processTaskOrWait();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                } catch (Exception e) {
                    logger.error("Error in worker {} main loop", workerId, e);
                    registry.markFailed(workerId);
                    state.set(WorkerState.FAILED);
                    break;
                }
            }

            heartbeat.shutdown();
            logger.info("Worker {} ({}) main loop ended", workerId, type);
        }, "worker-main-" + type.getName() + "-" + workerId);
        runningThread.setDaemon(true);
        runningThread.start();
    }

    /**
     * Process a task or wait for one
     */
    private void processTaskOrWait() throws InterruptedException {
        // Check mailbox for messages first
        Optional<MailboxMessage> msg = mailbox.receive();
        if (msg.isPresent()) {
            handleMessage(msg.get());
            return;
        }

        // Then check task queue
        Task task = taskQueue.poll(100, TimeUnit.MILLISECONDS);
        if (task != null) {
            registry.markRunning(workerId);
            processTask(task);
        } else {
            state.set(WorkerState.IDLE);
        }
    }

    /**
     * Handle incoming message
     */
    private void handleMessage(MailboxMessage msg) {
        logger.debug("Worker {} received message type {} from {}", workerId, msg.type(), msg.from());
        switch (msg.type()) {
            case TASK -> {
                if (msg.payload() instanceof Task task) {
                    processTask(task);
                }
            }
            case SHUTDOWN -> {
                shuttingDown = true;
            }
            case HEARTBEAT -> {
                // Already handled by scheduled task
            }
            default -> logger.debug("Unhandled message type {} for worker {}", msg.type(), workerId);
        }
    }

    /**
     * Process a task
     */
    private void processTask(Task task) {
        logger.info("Worker {} processing task {} ({})", workerId, task.taskId(), task.type());
        try {
            Task result = doProcessTask(task);
            mailbox.sendTo("leader", MailboxMessage.MessageType.RESULT, result);
            logger.info("Worker {} completed task {}", workerId, task.taskId());
        } catch (Exception e) {
            logger.error("Worker {} failed task {}", workerId, task.taskId(), e);
            mailbox.sendTo("leader", MailboxMessage.MessageType.ERROR,
                task.withError(e.getMessage()));
        }
    }

    /**
     * Submit a task to this worker
     */
    public void submitTask(Task task) {
        taskQueue.offer(task);
        logger.debug("Worker {} received task {}", workerId, task.taskId());
    }

    /**
     * Shutdown the worker gracefully
     */
    public void shutdown() {
        if (state.get() == WorkerState.SHUTTING_DOWN || state.get() == WorkerState.TERMINATED) {
            return;
        }
        shuttingDown = true;
        state.set(WorkerState.SHUTTING_DOWN);
        logger.info("Worker {} shutting down", workerId);

        if (executor != null) {
            executor.shutdown();
            try {
                if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                    executor.shutdownNow();
                }
            } catch (InterruptedException e) {
                executor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }

        mailbox.stopPolling();
        registry.deregister(workerId);
        state.set(WorkerState.TERMINATED);
        logger.info("Worker {} terminated", workerId);
    }

    // Abstract methods for subclasses

    /**
     * Perform worker-specific initialization
     */
    protected abstract void doInitialize();

    /**
     * Process a task and return result
     */
    protected abstract Task doProcessTask(Task task);

    // Getters

    public String getWorkerId() {
        return workerId;
    }

    public WorkerType getType() {
        return type;
    }

    public WorkerState getState() {
        return state.get();
    }

    public Mailbox getMailbox() {
        return mailbox;
    }

    public enum WorkerState {
        CREATED,
        INITIALIZING,
        REGISTERED,
        RUNNING,
        IDLE,
        SHUTTING_DOWN,
        TERMINATED,
        FAILED
    }
}
