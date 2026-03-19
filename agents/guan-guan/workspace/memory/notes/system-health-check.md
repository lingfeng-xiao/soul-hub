# 系统健康检查最佳实践

## 核心要点

### 1. Cron 任务检查
- 每20分钟检查所有定时任务状态
- 关注：lastRunStatus、lastDurationMs、consecutiveErrors
- 异常指标：连续失败 > 0、持续时间异常增长

### 2. Agent 注册检查
- 验证 agents.list 完整性
- 检查 workspace 路径是否存在
- 验证 subagents.allowAgents 配置

### 3. 配置文件验证
- 运行 gateway config.get 检查配置有效性
- 关注 warnings 和 issues 字段
- 检查版本一致性

### 4. 影响评估矩阵

| 类型 | 示例 | 行动 |
|------|------|------|
| 高 | 核心配置删除、Agent注册变更 | 等待用户批准 |
| 中 | 非核心配置调整、文档优化 | 完成后报告 |
| 低 | 笔记整理、临时任务清理 | 立即执行 |

## 应用场景

管管每20分钟自动执行健康检查，确保系统稳定运行。

---
*2026-03-19 记录*
