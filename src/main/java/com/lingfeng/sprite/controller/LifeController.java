package com.lingfeng.sprite.controller;

import com.lingfeng.sprite.controller.dto.AutonomyStatusResponse;
import com.lingfeng.sprite.controller.dto.LifeCommandRequest;
import com.lingfeng.sprite.controller.dto.LifeCommandResponse;
import com.lingfeng.sprite.controller.dto.LifeJournalEntryDto;
import com.lingfeng.sprite.domain.snapshot.LifeSnapshot;
import com.lingfeng.sprite.domain.snapshot.LifeSnapshotService;
import com.lingfeng.sprite.life.AutonomyPolicyService;
import com.lingfeng.sprite.life.LifeCommandService;
import com.lingfeng.sprite.life.LifeJournalService;
import com.lingfeng.sprite.life.LifeRuntimeStateService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/life")
public class LifeController {

    private final LifeSnapshotService lifeSnapshotService;
    private final LifeCommandService lifeCommandService;
    private final AutonomyPolicyService autonomyPolicyService;
    private final LifeJournalService lifeJournalService;
    private final LifeRuntimeStateService lifeRuntimeStateService;

    public LifeController(
            LifeSnapshotService lifeSnapshotService,
            LifeCommandService lifeCommandService,
            AutonomyPolicyService autonomyPolicyService,
            LifeJournalService lifeJournalService,
            LifeRuntimeStateService lifeRuntimeStateService
    ) {
        this.lifeSnapshotService = lifeSnapshotService;
        this.lifeCommandService = lifeCommandService;
        this.autonomyPolicyService = autonomyPolicyService;
        this.lifeJournalService = lifeJournalService;
        this.lifeRuntimeStateService = lifeRuntimeStateService;
    }

    @GetMapping("/snapshot")
    public ResponseEntity<LifeSnapshot> getSnapshot() {
        return ResponseEntity.ok(lifeSnapshotService.generateSnapshot());
    }

    @PostMapping("/commands")
    public ResponseEntity<LifeCommandResponse> executeCommand(@RequestBody LifeCommandRequest request) {
        return ResponseEntity.ok(lifeCommandService.execute(request));
    }

    @GetMapping("/autonomy/status")
    public ResponseEntity<AutonomyStatusResponse> getAutonomyStatus() {
        return ResponseEntity.ok(autonomyPolicyService.getStatus());
    }

    @PostMapping("/autonomy/pause")
    public ResponseEntity<AutonomyStatusResponse> pauseAutonomy() {
        return ResponseEntity.ok(autonomyPolicyService.pause());
    }

    @PostMapping("/autonomy/resume")
    public ResponseEntity<AutonomyStatusResponse> resumeAutonomy() {
        return ResponseEntity.ok(autonomyPolicyService.resume());
    }

    @GetMapping("/journal")
    public ResponseEntity<List<LifeJournalEntryDto>> getJournal(
            @RequestParam(name = "limit", defaultValue = "20") int limit
    ) {
        return ResponseEntity.ok(lifeJournalService.getRecentEntries(limit).stream()
                .map(LifeJournalEntryDto::from)
                .toList());
    }

    @PostMapping("/reset")
    public ResponseEntity<LifeSnapshot> resetLife() {
        lifeRuntimeStateService.resetLifeState();
        return ResponseEntity.ok(lifeSnapshotService.generateSnapshot());
    }
}
