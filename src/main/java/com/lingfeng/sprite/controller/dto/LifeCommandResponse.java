package com.lingfeng.sprite.controller.dto;

import com.lingfeng.sprite.domain.command.ImpactReport;
import com.lingfeng.sprite.domain.snapshot.LifeSnapshot;

public record LifeCommandResponse(
        LifeCommandResultDto commandResult,
        ImpactReport impactReport,
        LifeSnapshot lifeSnapshot
) {}
