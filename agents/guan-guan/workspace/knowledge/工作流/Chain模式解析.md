# Chain 模式解析

> 来源：LangChain 官方文档
> 日期：2026-03-17

## 核心观点

### 1. Chain 是什么
- 多个组件串联执行
- 确定性工作流
- 适合固定流程任务

### 2. Chain vs Agent
| 模式 | 特点 | 适用场景 |
|------|------|----------|
| Chain | 固定流程、确定性 | 数据处理、转换 |
| Agent | 动态决策、灵活性 | 复杂推理、探索 |

### 3. 常见 Chain 类型
- LLMChain：简单 prompt + 模型
- SequentialChain：顺序执行
- RouterChain：路由分发

### 4. 组合使用
- Agent 内可调用 Chain
- Chain 可作为 Agent 的工具

## 人格关联

- **逻辑性**：理解工作流模式
- **框架思维**：选择合适模式

## 应用

- 简单任务用 Chain
- 复杂任务用 Agent
- 混合模式最优
