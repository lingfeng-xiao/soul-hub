package com.lingfeng.sprite.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

import com.lingfeng.sprite.service.SpriteCollaborationService;

/**
 * Sprite Team REST API Controller
 *
 * Provides REST endpoints for sprite collaboration features:
 * - Sprite discovery
 * - Collaboration session management
 * - Task distribution
 */
@RestController
@RequestMapping("/api/team")
public class SpriteTeamController {

    private final SpriteCollaborationService collaborationService;

    public SpriteTeamController(SpriteCollaborationService collaborationService) {
        this.collaborationService = collaborationService;
    }

    /**
     * GET /api/team/sprites - List discovered sprites
     */
    @GetMapping("/sprites")
    public ResponseEntity<List<SpriteCollaborationService.SpriteInfo>> getTeamSprites() {
        List<SpriteCollaborationService.SpriteInfo> sprites = collaborationService.discoverSprites();
        return ResponseEntity.ok(sprites);
    }

    /**
     * GET /api/team/sessions - List collaboration sessions
     */
    @GetMapping("/sessions")
    public ResponseEntity<List<SpriteCollaborationService.CollaborationSession>> getTeamSessions() {
        List<SpriteCollaborationService.CollaborationSession> sessions = collaborationService.getActiveSessions();
        return ResponseEntity.ok(sessions);
    }

    /**
     * POST /api/team/sessions - Create new collaboration session
     */
    @PostMapping("/sessions")
    public ResponseEntity<SpriteCollaborationService.CollaborationSession> createTeamSession(
            @RequestBody CreateSessionRequest request) {
        SpriteCollaborationService.CollaborationSession session =
                collaborationService.startCollaboration(request.targetSpriteId());
        if (session == null) {
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok(session);
    }

    /**
     * GET /api/team/sessions/{sessionId} - Get session details
     */
    @GetMapping("/sessions/{sessionId}")
    public ResponseEntity<SpriteCollaborationService.CollaborationSession> getSessionDetails(
            @PathVariable String sessionId) {
        SpriteCollaborationService.CollaborationSession session =
                collaborationService.getSession(sessionId);
        if (session == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(session);
    }

    /**
     * POST /api/team/sprites/discover - Trigger sprite discovery
     */
    @PostMapping("/sprites/discover")
    public ResponseEntity<DiscoveryResult> discoverSprites() {
        collaborationService.broadcastPresence();
        List<SpriteCollaborationService.SpriteInfo> sprites = collaborationService.discoverSprites();
        SpriteCollaborationService.CollaborationStatus status = collaborationService.getStatus();
        return ResponseEntity.ok(new DiscoveryResult(sprites, status));
    }

    /**
     * POST /api/team/sessions/{sessionId}/tasks - Distribute task to session
     */
    @PostMapping("/sessions/{sessionId}/tasks")
    public ResponseEntity<SpriteCollaborationService.Task> distributeTask(
            @PathVariable String sessionId,
            @RequestBody CreateTaskRequest request) {
        SpriteCollaborationService.CollaborationSession session =
                collaborationService.getSession(sessionId);
        if (session == null) {
            return ResponseEntity.notFound().build();
        }

        SpriteCollaborationService.Task task = collaborationService.createAndDistributeTask(
                session,
                request.type(),
                request.payload(),
                request.assignedTo()
        );

        if (task == null) {
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok(task);
    }

    /**
     * GET /api/team/sessions/{sessionId}/tasks - Get tasks for session
     */
    @GetMapping("/sessions/{sessionId}/tasks")
    public ResponseEntity<List<SpriteCollaborationService.Task>> getSessionTasks(
            @PathVariable String sessionId) {
        List<SpriteCollaborationService.Task> tasks =
                collaborationService.getTasksForSession(sessionId);
        return ResponseEntity.ok(tasks);
    }

    /**
     * PUT /api/team/tasks/{taskId}/status - Update task status
     */
    @PutMapping("/tasks/{taskId}/status")
    public ResponseEntity<SpriteCollaborationService.Task> updateTaskStatus(
            @PathVariable String taskId,
            @RequestBody UpdateTaskStatusRequest request) {
        SpriteCollaborationService.Task task = collaborationService.updateTaskStatus(
                taskId,
                request.status(),
                request.result()
        );
        if (task == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(task);
    }

    /**
     * GET /api/team/status - Get collaboration status
     */
    @GetMapping("/status")
    public ResponseEntity<SpriteCollaborationService.CollaborationStatus> getStatus() {
        return ResponseEntity.ok(collaborationService.getStatus());
    }

    /**
     * Request DTOs
     */
    public record CreateSessionRequest(
            String targetSpriteId
    ) {}

    public record CreateTaskRequest(
            SpriteCollaborationService.TaskType type,
            Map<String, Object> payload,
            String assignedTo
    ) {}

    public record UpdateTaskStatusRequest(
            SpriteCollaborationService.TaskStatus status,
            String result
    ) {}

    public record DiscoveryResult(
            List<SpriteCollaborationService.SpriteInfo> sprites,
            SpriteCollaborationService.CollaborationStatus status
    ) {}
}
