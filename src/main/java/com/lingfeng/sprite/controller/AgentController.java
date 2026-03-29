package com.lingfeng.sprite.controller;

import com.lingfeng.sprite.agent.AgentRegistry;
import com.lingfeng.sprite.agent.LeaderAgent;
import com.lingfeng.sprite.agent.Mailbox;
import com.lingfeng.sprite.agent.WorkerType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * REST controller for multi-agent system management
 */
@RestController
@RequestMapping("/api/agent")
public class AgentController {
    private static final Logger logger = LoggerFactory.getLogger(AgentController.class);

    private final AgentRegistry registry;
    private final LeaderAgent leaderAgent;

    public AgentController(AgentRegistry registry, LeaderAgent leaderAgent) {
        this.registry = registry;
        this.leaderAgent = leaderAgent;
    }

    /**
     * Get all registered workers
     */
    @GetMapping("/workers")
    public ResponseEntity<Map<String, Object>> getWorkers() {
        Map<String, Object> response = new HashMap<>();
        List<Map<String, Object>> workers = registry.getAllWorkers().stream()
            .map(w -> {
                Map<String, Object> info = new HashMap<>();
                info.put("workerId", w.getWorkerId());
                info.put("type", w.getType().name());
                info.put("state", w.getState().name());
                info.put("registeredAt", w.getRegisteredAt().toString());
                return info;
            })
            .toList();

        response.put("workers", workers);
        response.put("totalCount", workers.size());
        response.put("summary", registry.getStatusSummary());
        return ResponseEntity.ok(response);
    }

    /**
     * Get worker by ID
     */
    @GetMapping("/workers/{workerId}")
    public ResponseEntity<Map<String, Object>> getWorker(@PathVariable String workerId) {
        return registry.getWorker(workerId)
            .map(w -> {
                Map<String, Object> info = new HashMap<>();
                info.put("workerId", w.getWorkerId());
                info.put("type", w.getType().name());
                info.put("state", w.getState().name());
                info.put("registeredAt", w.getRegisteredAt().toString());
                return ResponseEntity.ok(info);
            })
            .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Get workers by type
     */
    @GetMapping("/workers/type/{type}")
    public ResponseEntity<Map<String, Object>> getWorkersByType(@PathVariable String type) {
        try {
            WorkerType workerType = WorkerType.fromName(type);
            List<Map<String, Object>> workers = registry.getWorkersByType(workerType).stream()
                .map(w -> {
                    Map<String, Object> info = new HashMap<>();
                    info.put("workerId", w.getWorkerId());
                    info.put("state", w.getState().name());
                    info.put("registeredAt", w.getRegisteredAt().toString());
                    return info;
                })
                .toList();

            Map<String, Object> response = new HashMap<>();
            response.put("type", type);
            response.put("workers", workers);
            response.put("count", workers.size());
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Get agent registry status
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getStatus() {
        Map<String, Object> response = new HashMap<>();
        response.put("summary", registry.getStatusSummary());
        response.put("workerCount", registry.getWorkerCount());
        response.put("perceptionCount", registry.getWorkerCount(WorkerType.PERCEPTION));
        response.put("cognitionCount", registry.getWorkerCount(WorkerType.COGNITION));
        response.put("actionCount", registry.getWorkerCount(WorkerType.ACTION));
        response.put("leaderRunning", leaderAgent != null && leaderAgent.isRunning());
        response.put("lastCycleTime", leaderAgent.getLastCycleTime() != null ?
            leaderAgent.getLastCycleTime().toString() : null);
        return ResponseEntity.ok(response);
    }

    /**
     * Get leader info
     */
    @GetMapping("/leader")
    public ResponseEntity<Map<String, Object>> getLeader() {
        if (leaderAgent == null) {
            return ResponseEntity.notFound().build();
        }
        Map<String, Object> response = new HashMap<>();
        response.put("leaderId", leaderAgent.getLeaderId());
        response.put("running", leaderAgent.isRunning());
        response.put("lastCycleTime", leaderAgent.getLastCycleTime() != null ?
            leaderAgent.getLastCycleTime().toString() : null);
        response.put("workerCount", leaderAgent.getAllWorkers().size());
        return ResponseEntity.ok(response);
    }

    /**
     * Cleanup dead workers
     */
    @PostMapping("/cleanup")
    public ResponseEntity<Map<String, Object>> cleanup() {
        int before = registry.getWorkerCount();
        registry.cleanupDeadWorkers();
        int after = registry.getWorkerCount();

        Map<String, Object> response = new HashMap<>();
        response.put("before", before);
        response.put("after", after);
        response.put("cleanedUp", before - after);
        return ResponseEntity.ok(response);
    }

    /**
     * Health check
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "UP");
        response.put("registry", registry.getStatusSummary());
        response.put("leaderRunning", leaderAgent != null && leaderAgent.isRunning());
        return ResponseEntity.ok(response);
    }
}
