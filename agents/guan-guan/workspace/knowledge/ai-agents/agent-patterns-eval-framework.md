# Agent 工作流模式与评估框架

> 来源: Anthropic Engineering Blog (2024-2026)
> 日期: 2026-03-18

## 核心原则：简单优先

**最成功的 Agent 实现使用简单、可组合的模式，而非复杂框架。**

> "We've worked with dozens of teams building LLM agents across industries. Consistently, the most successful implementations use simple, composable patterns rather than complex frameworks."

---

## 五种核心工作流模式

按复杂度从低到高排序：

### 1. Prompt Chaining（提示链）
- 将任务分解为顺序步骤，每个 LLM 调用处理前一个的输出
- 适用场景：可以轻松分解为固定子任务的情况
- 示例：写大纲 → 检查大纲 → 写文档

### 2. Routing（路由）
- 分类输入并定向到专用后续任务
- 适用场景：有明显类别区分的复杂任务
- 示例：客服问题分类 → 不同处理流程；简单问题用小模型，复杂问题用大模型

### 3. Parallelization（并行化）
- 同时运行多个独立任务，结果聚合
- 两种形式：
  - **Sectioning**：分解独立子任务并行执行
  - **Voting**：同一任务运行多次获取多样输出
- 适用场景：可并行提速或需要多视角/多尝试获得高置信度结果

### 4. Orchestrator-Workers（编排器-工作者）
- 中央 LLM 动态分解任务，委托给工作 LLM，结果合成
- 关键区别：任务不是预定义的，由编排器根据输入动态决定
- 适用场景：无法预测所需子任务的复杂任务（如代码修改）

### 5. Evaluator-Optimizer（评估器-优化器）
- 一个 LLM 生成响应，另一个提供评估和反馈，循环迭代
- 适用场景：有明确评估标准，迭代改进有可衡量价值
- 示例：文学翻译、复杂搜索任务

---

## Agent vs Workflow

| 特征 | Workflow | Agent |
|------|----------|-------|
| 控制方式 | 预定义代码路径 | LLM 动态控制 |
| 灵活性 | 低 | 高 |
| 预测性 | 高 | 低 |
| 适用场景 | 明确任务 | 开放性问题 |

**何时使用 Agent**：
- 难以/无法预测所需步骤数
- 无法硬编码固定路径
- LLM 需要运行多轮
- 必须在可信环境中扩展

---

## Agent 评估框架

### 核心概念

| 概念 | 定义 |
|------|------|
| Task | 单个测试，有定义输入和成功标准 |
| Trial | 一次尝试（因为模型输出有变化，通常运行多次） |
| Grader | 评分逻辑，一个任务可有多个 graders |
| Transcript | 完整记录（输出、工具调用、推理、中间结果） |
| Outcome | 试验结束时的最终状态 |
| Evaluation Harness | 运行评估的基础设施 |

### 三种 Grader 类型

| 类型 | 优势 | 劣势 |
|------|------|------|
| **Code-based** | 快速、便宜、客观、可复现 | 脆弱、对有效变化不宽容 |
| **Model-based** | 灵活、可扩展、能捕捉细微差别 | 非确定性、昂贵、需要校准 |
| **Human** | 金标准质量 | 昂贵、慢 |

### Capability vs Regression Evals

| 类型 | 问题 | 预期起始通过率 |
|------|------|---------------|
| **Capability** | "这个 Agent 能做什么？" | 低（让团队有目标攀登） |
| **Regression** | "这个 Agent 还能处理之前的任务吗？" | 接近 100%（防止退化） |

**演进路径**：Capability eval 高分后 → 毕业为 Regression suite

---

## 实践建议

1. **从简单开始**：先优化单次 LLM 调用 + retrieval + in-context examples
2. **只在需要时增加复杂度**：当简单方案不足时
3. **框架是起步工具**：但到生产时减少抽象层
4. **保持 ACI（Agent-Computer Interface）清晰**：工具文档完整且经过测试

---

## 相关笔记

- [[ai-agent-infra-noise]] - 基础设施噪声对 Agent 评估的影响
- [[multi-agent-research-system]] - 多 Agent 研究系统构建
