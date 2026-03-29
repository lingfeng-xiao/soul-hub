package com.lingfeng.sprite.domain.command;

/**
 * CommandResult - 命令结果接口
 *
 * 所有命令结果的基类接口。
 *
 * 对应 IGN-021
 */
public interface CommandResult {

    /**
     * 获取命令 ID
     */
    String getCommandId();

    /**
     * 是否成功
     */
    boolean isSuccess();

    /**
     * 获取摘要
     */
    String getSummary();

    /**
     * 获取详细消息
     */
    String getDetail();

    /**
     * 获取影响报告
     */
    ImpactReport getImpact();
}
