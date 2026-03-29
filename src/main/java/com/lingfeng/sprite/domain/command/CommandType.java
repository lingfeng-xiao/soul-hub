package com.lingfeng.sprite.domain.command;

/**
 * CommandType - 命令类型枚举
 *
 * 定义 Console 支持的所有命令类型。
 *
 * 对应 IGN-021
 */
public enum CommandType {
    ASK,           // 提问/问答
    TASK,          // 任务执行
    RESEARCH,       // 研究/探索
    DECISION,      // 决策支持
    LEARNING,      // 学习支持
    ACTION         // 操作执行
}
