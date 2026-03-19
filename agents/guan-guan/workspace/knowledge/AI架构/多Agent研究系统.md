# 多Agent研究系统（Anthropic）

> 来源：Anthropic Engineering Blog
> 日期：2026-03-17

## 核心观点

### 1. 多Agent系统优势
- 研究工作难以预测所需步骤
- 过程动态且路径依赖
- Agent 灵活性好，可多轮自主决策
- 并行探索不同方向

### 2. 压缩即搜索
- 搜索本质：从海量信息中提炼洞察
- Subagent 通过并行 + 独立上下文窗口实现压缩
- 每个 subagent 提供关注点分离

### 3. 规模效应
- 单一智能有上限
- 多Agent协作实现指数级能力提升
- 类似人类社会集体智慧

### 4. 性能数据
- 多Agent系统（Opus 4 + Sonnet 4 subagents）vs 单Agent Opus 4
- 内部研究评估：+90.2% 性能提升
- Token 使用量解释了 95% 的性能差异

### 5. 架构模式
- Lead Agent：规划研究过程
- Subagents：并行搜索不同方向
- 最后压缩关键信息给 Lead Agent

## 人格关联

- **系统思考**：理解多Agent协作
- **分析力**：性能归因分析

## 应用

- OpenClaw 多Agent管理
- 复杂任务分解并行处理
