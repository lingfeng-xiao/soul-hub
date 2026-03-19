# AI Agent 编排与任务分发模式

## 核心要点
**主从架构 + 消息协议** 是多 Agent 协作的核心模式。

## 关键模式

### 1. 主从架构
- **主 Agent**（如管管）：负责任务分发、结果汇总、决策判断
- **子 Agent**（如小艺/后助）：负责执行具体任务
- 通信通过 `sessions_spawn` 或 `sessions_send` 进行

### 2. 消息协议
参考 `docs/SUBAGENT_MSG_PROTOCOL.md`：
- 标准化消息格式
- 包含上下文、期望输出格式
- 支持超时和错误处理

### 3. 任务分发策略
| 策略 | 适用场景 |
|------|----------|
| 串行 | 有依赖关系的任务 |
| 并行 | 独立任务，汇总结果 |
| 链式 | A→B→C 传递结果 |

## 应用建议

1. **超时控制**：设置 `timeoutSeconds`，避免子 Agent 无限等待
2. **错误处理**：子 Agent 失败时，主 Agent 决定重试或降级
3. **状态追踪**：使用 `sessions_list` 监控子 Agent 活跃状态

## 相关工具
- `sessions_spawn` - 创建子会话
- `sessions_send` - 发送消息到子会话
- `sessions_list` - 列出活跃会话
