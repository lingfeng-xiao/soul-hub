package com.lingfeng.sprite.controller.dto;

import java.time.Instant;
import java.util.List;

public record AutonomyStatusResponse(
        String mode,
        boolean paused,
        boolean allowInternal,
        boolean allowReadonly,
        boolean allowMutating,
        float autonomyFactor,
        String awarenessLevel,
        long totalDecisions,
        long autonomousDecisions,
        List<String> recentActions,
        Instant updatedAt
) {}
