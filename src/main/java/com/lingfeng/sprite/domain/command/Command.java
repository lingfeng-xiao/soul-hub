package com.lingfeng.sprite.domain.command;

import java.time.Instant;

/**
 * Command - 命令接口
 *
 * 所有命令的基类接口。
 * 命令是用户与 Sprite 交互的基本单位。
 *
 * 对应 IGN-021
 */
public interface Command {

    /**
     * 获取命令 ID
     */
    String getCommandId();

    /**
     * 获取命令类型
     */
    CommandType getType();

    /**
     * 获取命令描述
     */
    String getDescription();

    /**
     * 获取创建时间
     */
    Instant getCreatedAt();

    /**
     * 获取执行状态
     */
    CommandStatus getStatus();

    /**
     * 命令执行状态
     */
    enum CommandStatus {
        PENDING,     // 待执行
        EXECUTING,   // 执行中
        COMPLETED,   // 已完成
        FAILED,      // 失败
        CANCELLED    // 已取消
    }
}
