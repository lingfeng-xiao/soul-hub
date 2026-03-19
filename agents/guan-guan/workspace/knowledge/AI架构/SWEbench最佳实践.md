# SWE-bench 最佳实践

> 来源：Anthropic Engineering Blog
> 日期：2026-03-17

## 核心观点

### 1. SWE-bench
- AI 评估基准：解决真实软件工程任务
- 测试模型解决 GitHub issues
- 用真实单元测试评分

### 2. Agent vs Model
- 评估的是整个 Agent 系统
- 包括：模型 + 脚手架（scaffolding）
- 脚手架影响性能巨大

### 3. Claude 3.5 Sonnet 成绩
- SWE-bench Verified：49%
- 超越之前的 45%
- Agent 脚手架优化是关键

### 4. 设计原则
- 给模型最多控制
- 保持脚手架最小化
- 工具：Bash + Edit
- 模型自主决定步骤

### 5. SWE-bench Verified
- 原始数据集有些问题无法解决
- Verified 版本人工审核确保可解
- 500 题子集

## 人格关联

- **理性**：脚手架重要性
- **分析力**：系统评估思维

## 应用

- Agent 开发：脚手架优化
- 编码任务自动化
