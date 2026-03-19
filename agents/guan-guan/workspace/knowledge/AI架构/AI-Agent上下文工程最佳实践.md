# AI Agent 上下文工程最佳实践

> 来源: Anthropic Engineering - Effective context engineering for AI agents (2025-09-29)

## 核心理念

**上下文工程 = 提示工程的进化**

| 提示工程 | 上下文工程 |
|---------|-----------|
| 写好 prompt 指令 | 管理 entire context state |
| 一次性任务 | 多轮迭代、长时间运行 |
| 离散问题 | 持续演化的信息宇宙 |

**定义**: 在有限上下文窗口中，精选高信号 token 集合，最大化期望行为概率

## 为什么需要上下文工程

### 1. 注意力预算有限

> "像人类有有限工作记忆，LLM 也有注意力预算"

- 上下文越长，注意力越分散
- [Context Rot 现象](https://research.trychroma.com/context-rot): token 越多，召回率下降
- 每个新 token 都消耗注意力预算

### 2. Transformer 架构约束

- **n² 复杂度**: n 个 token 有 n² 配对关系
- 位置编码插值有精度损失
- 长序列训练数据稀缺

### 3. 性能梯度而非硬边界

- 模型在长上下文仍可用，但精度下降
- 信息检索和长程推理首先受影响

## 上下文构成

```
Context = System Prompt + Tools + MCP + External Data + Message History + ...
```

## 最佳实践

### 1. System Prompt 设计

**黄金法则**: 用最少的 token 提供完整行为描述

**结构化建议**:
```
<background_information>  # 背景
<instructions>           # 核心指令
## Tool guidance          # 工具使用指引
## Output description    # 输出格式描述
```

**两种失败模式**:

| 极端 | 问题 | 解决 |
|------|------|------|
| 过度设计 | 复杂 if-else 逻辑 | 简洁原则 + fallback 机制 |
| 过度模糊 | 缺乏具体指引 | 提供具体 heuristic |

**实践建议**:
1. 先用最小 prompt 测试
2. 根据失败模式逐步添加
3. 添加时明确"修复什么问题"

### 2. 工具设计 (Tools)

**核心原则**: 工具即 Agent 与环境的接口

| 原则 | 说明 |
|------|------|
| 自包含 | 功能单一，不与其他工具重叠 |
| 清晰边界 | 人类能明确判断用哪个工具 |
| Token 高效 | 返回信息精炼，不过度冗余 |
| 错误健壮 | 优雅处理异常情况 |

**常见失败模式**:
- 工具过多 → Agent 决策困难
- 工具功能重叠 → 行为不稳定
- 参数描述模糊 → Agent 误解意图

### 3. 消息历史管理

**分层策略**:

```
Tier 1: 当前任务直接相关
Tier 2: 项目级上下文
Tier 3: 长期记忆 (摘要化)
```

**滚动窗口**:
- 保留最近 N 轮
- 关键决策点保留完整
- 周期性摘要压缩

### 4. 外部数据接入

**RAG vs 全部加载**:

| 方案 | 适用场景 |
|------|---------|
| 全部加载 | 小规模、高频访问 |
| RAG | 大规模、低频访问 |
| 混合 | 热数据加载 + 冷数据 RAG |

## 注意力优化技巧

### 1. 精炼优先

```python
# ❌ 冗余
"The user has been working on this project for 3 days.
 They have previously written 500 lines of code..."

# ✅ 精炼
"Project: C Compiler | Status: 85% | Focus: ARM64"
```

### 2. 结构化标记

```
## 当前任务
### 上下文
- key1: value1
- key2: value2

### 已完成
- [x] Task A
- [x] Task B
```

### 3. 分块加载

```python
# 不一次加载全部
def get_context(task):
    if is_simple_task(task):
        return load_recent_messages(5)
    elif is_complex_task(task):
        return load_summary() + load_recent_messages(20)
```

## 架构决策清单

- [ ] 最小可行 prompt 是什么？
- [ ] 哪些工具真正必要？
- [ ] 消息历史需要保留多少？
- [ ] 外部数据何时加载？
- [ ] 上下文超限时优先保留什么？

## 延伸阅读

- [Building Effective Agents](https://www.anthropic.com/engineering/building-effective-agents)
- [Writing Tools for Agents](https://www.anthropic.com/engineering/writing-tools-for-agents)
- [MCP 文档](https://modelcontextprotocol.io/docs/getting-started/intro)

---

*上下文是有限资源，需要精心工程化*
