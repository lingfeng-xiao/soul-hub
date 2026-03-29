package com.lingfeng.sprite.controller.dto;

import com.lingfeng.sprite.domain.command.CommandResult;

public record LifeCommandResultDto(
        String commandId,
        String type,
        String summary,
        String detail,
        boolean success
) {
    public static LifeCommandResultDto from(String type, CommandResult result) {
        return new LifeCommandResultDto(
                result.getCommandId(),
                type,
                result.getSummary(),
                result.getDetail(),
                result.isSuccess()
        );
    }
}
