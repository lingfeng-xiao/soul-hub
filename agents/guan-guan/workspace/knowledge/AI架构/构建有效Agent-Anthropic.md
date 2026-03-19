# 构建有效 Agent（Anthropic）

> 来源：Anthropic Engineering Blog
> 日期：2026-03-17

## 核心观点

### 1. 简单可组合 > 复杂框架
- 最成功的实现使用简单、可组合的模式
- 不是复杂框架或专用库
- 最佳实践：用最简单方案，需要才增加复杂度

### 2. Workflow vs Agent
| 类型 | 特点 | 适用 |
|------|------|------|
| **Workflow** | 预定义代码路径 orchestration | 确定性任务 |
| **Agent** | 模型动态控制过程和工具使用 | 灵活性任务 |

### 3. 何时用/不用 Agent
- 优先用最简单的方案
- Agent trade-off：延迟和成本换更好性能
- 优化单次 LLM 调用 + 检索 + 上下文示例往往足够

### 4. 框架建议
- 可用：Claude Agent SDK、AWS Strands、Rivet、Vellum
- 问题：额外抽象层掩盖底层 prompt/响应
- 建议：从直接用 LLM API 开始，几行代码实现模式

### 5. 构建块：增强型 LLM
- 基础：检索 + 工具 + 记忆
- 模型可主动：生成搜索查询、选择工具、决定保留信息
- MCP：Model Context Protocol 统一工具集成

### 6. 常见工作流模式
- **Prompt Chaining**：分解为顺序步骤
- **Routing**：分类后分发
- **Parallelization**：并行处理
- **Orchestrator-workers**：协调器分发子任务
- **Evaluator-evaluator**：评估循环优化

## 人格关联

- **理性**：不盲目用 Agent
- **逻辑性**：理解模式选择
- **专业**：掌握最佳实践

## 应用

- 简单任务：单次 LLM 调用
- 复杂但固定：Workflow
- 复杂且灵活：Agent
