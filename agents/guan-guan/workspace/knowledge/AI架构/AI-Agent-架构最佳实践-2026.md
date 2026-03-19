# AI Agent 架构最佳实践 (2026)

> 来源：Anthropic + Cursor 官方文档 2026-03-18

---

## 核心理念

**简单 > 复杂**  
最成功的实现使用简单、可组合的模式，而不是复杂框架。

---

## 架构区分

| 类型 | 定义 | 适用场景 |
|------|------|----------|
| **Workflows** | 预定义代码路径编排 LLM 和工具 | 任务明确、需要可预测性 |
| **Agents** | LLM 动态指导自己的流程和工具使用 | 灵活性、模型驱动决策 |

---

## Agent 构建块

### 1. 增强型 LLM (Augmented LLM)
- **检索 (Retrieval)**: 动态获取相关信息
- **工具 (Tools)**: 生成自己的搜索查询、选择合适的工具
- **记忆 (Memory)**: 决定保留什么信息

### 2. MCP (Model Context Protocol)
- 简化与第三方工具的集成
- 单一客户端实现接入生态系统

---

## 主流 Agent 产品能力

### Claude Agent (Anthropic)
- ✅ 规划、行动、协作
- ✅ 客户支持、编码场景优化
- ✅ 代码流失减少 **3x**
- ✅ 诚实性、越狱抵抗、品牌安全最高

### Cursor Agent
- ✅ 理解代码库（不论大小）
- ✅ 子代理并行运行，每个用最佳模型
- ✅ 自定义嵌入模型 → 最佳召回率
- ✅ 完整开发周期：Plan → Design → Code → Debug
- ✅ Terminal / Git / Plugins / Skills / MCP

---

## 框架使用建议

### 何时用框架
- 简化标准底层任务（LLM 调用、工具定义、链式调用）
- 快速原型

### 何时不用框架
- 任务简单，单 LLM 调用 + 检索 + 上下文示例足够
- 需要调试底层 prompts 和 responses

### 建议
1. **从 LLM API 直接开始** — 多数模式几行代码可实现
2. **用框架时** — 确保理解底层代码，避免错误假设

---

## 工作流模式

### Prompt Chaining
将任务分解为序列步骤，每个 LLM 调用处理一个步骤。

### Router
根据输入类型选择不同的处理路径。

### Parallel
并行运行多个 LLM 调用，聚合结果。

### Orchestrator-Workers
编排者动态分配任务给 workers，汇总结果。

---

## 参考来源

- [Claude Agents](https://claude.com/solutions/agents)
- [Cursor Product](https://cursor.com/product)
- [Building Effective Agents - Anthropic](https://www.anthropic.com/engineering/building-effective-agents)

---

*分类: AI架构 | 来源: 官方文档 | 日期: 2026-03-18*
