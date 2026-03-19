# LangChain Agent 架构解析

> 来源：LangChain 官方文档
> 日期：2026-03-17

## 核心观点

### 1. Agent 抽象层级
- **LangChain**：快速构建，10行代码上手
- **LangGraph**：底层编排框架，高级定制
- **Deep Agents**："电池已备"，内置长对话压缩、虚拟文件系统、子Agent

### 2. 架构选择建议
| 场景 | 推荐 |
|------|------|
| 快速原型 | LangChain |
| 高级定制+工作流 | LangGraph |
| 生产级复杂Agent | Deep Agents |

### 3. 核心能力
- 持久化执行 (Durable execution)
- 流式输出 (Streaming)
- 人机交互 (Human-in-the-loop)
- 状态持久 (Persistence)

### 4. 可观测性
- LangSmith：追踪请求、调试行为、评估输出

## 人格关联

- **逻辑性**：理解工具层级
- **分析力**：根据场景选架构

## 应用场景

- OpenClaw Agent 开发可参考 LangChain 架构
- 多Agent协作 → LangGraph
