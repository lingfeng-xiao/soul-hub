# 构建高效 AI Agents - Anthropic 最佳实践

> 来源: Anthropic Engineering Blog
> 日期: 2026-03-18
> 关键词: AI Agent, 架构模式, 最佳实践

---

## 核心原则

### 1. 简单优先
- 最成功的实现使用**简单、可组合的模式**，而非复杂框架
- 从简单 Prompt 开始，只有在简单方案不够时才添加 Agent 系统
- 框架可能带来额外抽象层，掩盖底层问题

### 2. 三大核心原则
1. **保持简洁** - Agent 设计越简单越好
2. **优先透明度** - 明确展示 Agent 的规划步骤
3. **精心设计 ACI** - Agent-Computer Interface，工具文档要清晰

---

## 基础构建块: Augmented LLM

增强型 LLM = LLM + 检索 + 工具 + 记忆

关键实现要点：
- 根据具体用例定制能力
- 提供简洁、文档完善的接口

---

## 五大工作流模式

### 1. Prompt Chaining (提示链)
- 将任务分解为序列步骤，每个 LLM 处理前一个输出
- 适用场景：可以轻松分解为固定子任务的情况
- 示例：写营销文案 → 翻译成其他语言

### 2. Routing (路由)
- 分类输入并导向专门的子任务
- 适用场景：有明确分类的复杂任务
- 示例：客服问题分流到不同处理流程

### 3. Parallelization (并行化)
- Sectioning: 分解独立子任务并行运行
- Voting: 多次运行获取多样化输出
- 适用场景：可并行加速或有多个视角需求

### 4. Orchestrator-Workers (编排器-工作者)
- 中央 LLM 动态分解任务，委派给工作 LLM，合成结果
- 适用场景：无法预测所需子任务的复杂任务（如编码）

### 5. Evaluator-Optimizer (评估器-优化器)
- 一个 LLM 生成响应，另一个评估并反馈，循环迭代
- 适用场景：有明确评估标准，可迭代改进

---

## Agents 何时使用

Agent 与工作流的区别：
- **工作流**: 预定义代码路径
- **Agent**: LLM 动态指导自己的流程

使用 Agent 的场景：
- 开放性问题，难以预测所需步骤数
- 需要模型驱动的决策
- 可以在受信任环境中扩展

---

## 长期运行 Agent 挑战与解决方案

### 挑战
1. Agent 倾向于一次性完成太多（one-shot）
2. 后续 Agent 会过早宣布任务完成

### 解决方案: 两部分架构

#### Initializer Agent
- 设置初始环境
- 创建 feature_list.json（200+ 特性）
- 创建 claude-progress.txt 进度日志
- 初始化 git commit

#### Coding Agent
- 每次只做一个 feature
- 保持环境干净（可合并到主分支）
- 使用 git revert 恢复坏代码

### 关键实践
1. **Feature List**: 所有特性标记为 failing，Agent 只能改 passes 字段
2. **Git commit**: 每次进展后提交，保留历史
3. **Progress file**: 让新 session 快速了解状态
4. **自动化测试**: 使用 Puppeteer 等工具端到端测试

---

## 经验总结

> "成功不在于构建最复杂的系统，而在于构建适合你需求的那个。"

从简单开始，用评估优化，只有当简单方案不够时才使用多步骤 Agent 系统。
