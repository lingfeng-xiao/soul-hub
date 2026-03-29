package com.lingfeng.sprite.action;

/**
 * CompensationHandler - 补偿处理器接口
 */
public interface CompensationHandler {

    /**
     * 对失败的任务进行补偿
     *
     * @param failedTask 失败的任务
     *
     * 示例:
     * - 发送消息失败 → 撤回消息
     * - 创建资源失败 → 删除已创建资源
     * - 更新状态失败 → 恢复原状态
     */
    void compensate(ActionTask failedTask);
}
