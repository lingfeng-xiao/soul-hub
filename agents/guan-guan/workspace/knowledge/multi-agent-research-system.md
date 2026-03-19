# Multi-Agent 研究系统架构

> 来源: Anthropic Engineering Blog (2025-06)
> 主题: 多智能体研究系统的设计与经验

## 核心发现

**性能提升:**
- 多 agent 系统比单 agent 提升 **90.2%** (内部研究评估)
- Token 使用量解释 **95%** 的性能差异

## 为什么需要多 Agent

### 研究任务的特性
- 开放性问题，难以预测所需步骤
- 过程动态且路径依赖
- 需要灵活调整方法，发现新线索时转向探索

### 单 agent 的局限
- 线性、一次性流程无法处理
- 路径依赖：早期决策影响全局
- 上下文窗口有限

### 多 agent 的优势
- **并行探索**: subagent 并行处理不同方向
- **上下文分离**: 独立工具、prompt、探索轨迹
- **压缩**: 每个 subagent 独立探索后浓缩关键信息给主 agent

## 架构设计

### Orchestrator-Worker 模式

```
User Query
    ↓
Lead Agent (Orchestrator)
    ├── Subagent 1 (并行)
    ├── Subagent 2 (并行)
    └── Subagent N (并行)
    ↓
Citation Agent
    ↓
Final Answer
```

### 工作流程
1. **Lead Agent** 分析查询，制定策略
2. **创建 Subagents** 并行搜索不同方面
3. **Subagent** 迭代使用搜索工具，评估结果
4. **Lead Agent** 合成结果，决定是否需要更多研究
5. **Citation Agent** 添加引用

### Memory 持久化
- 上下文窗口超过 200K tokens 会截断
- 重要：保存研究计划到 Memory

## 何时使用多 Agent

### 适合的场景
- ✅ 大量并行化的任务
- ✅ 信息超过单 agent 上下文窗口
- ✅ 需要接口多个复杂工具

### 不适合的场景
- ❌ 需要所有 agent 共享同一上下文
- ❌ agent 之间有强依赖关系
- ❌ 大多数编码任务（并行化机会少）

## Token 消耗

| 类型 | Token 倍数 |
|------|-----------|
| 单次聊天 | 1x |
| 单 Agent | ~4x |
| 多 Agent 系统 | ~15x |

**结论**: 多 agent 系统需要任务价值足够高以抵消成本

## Prompt 工程经验

### 1. 像你的 agent 一样思考
- 用模拟测试 prompt 效果
- 观察 step-by-step 行为
- 立即发现失败模式

### 2. 教授协调者如何委派
- 避免简单查询生成 50 个 subagent
- 避免无休止搜索不存在的信息源
- 避免 agent 之间过度更新互相干扰

### 3. 关键原则
- 每个 agent 的 prompt 独立优化
- 协调者需要明确的终止条件
- 给 subagent 清晰的任务定义

## 模型选择

### 内部实践
- Lead agent: Claude Opus 4 (更强的推理)
- Subagents: Claude Sonnet 4 (更高效)

### 发现
- 模型升级带来的增益大于简单增加 token 预算
- Sonnet 4 > 双倍 Sonnet 3.7 token 预算

## 核心教训

> 一旦个体智能达到阈值，多 agent 系统成为扩展性能的关键方式。

就像人类：个体智商量变有限，但集体智能在信息时代指数级增长。

---
*日期: 2026-03-18*
