# 长时运行 Agent 的双 Agent 架构

> 来源: Anthropic Engineering - Effective harnesses for long-running agents (2025-11-26)

## 核心挑战

### 多上下文窗口问题

- Agent 必须以离散会话工作
- 每个新会话开始时没有之前的记忆
- 类似软件项目：每班工程师 arrives with no memory of previous shift
- 上下文窗口有限，复杂项目无法在单个窗口内完成

### 典型失败模式

**模式一：试图一次完成所有工作**
- Agent 试图 one-shot 完成整个应用
- 常常在实现中途耗尽上下文
- 下个会话需要从半完成且无文档的功能重新开始
- 浪费大量时间让基础应用重新运行

**模式二：过早宣布完成**
- 在一些功能完成后，后续 Agent 实例环顾四周
- 看到已有进展，宣布工作完成

## 双 Agent 解决方案

### 1. Initializer Agent（初始化 Agent）

第一个 Agent 会话使用专门 prompt：
- 设置初始环境
- 创建 `init.sh` 脚本
- 创建 `claude-progress.txt` 文件（记录 Agent 做了什么）
- 创建初始 git commit

### 2. Coding Agent（编码 Agent）

每个后续会话：
- 每次只做一个功能
- 增量进展
- 离开时保持环境整洁（clean state）
- 留下结构化更新供下一个会话使用

## 关键组件

### Feature List（功能列表）

Initializer Agent 编写全面的功能需求文件：
- 扩展用户的初始 prompt
- 数百个功能点（如："用户可以打开新聊天，输入查询，按回车，看到 AI 回复"）
- 初始全部标记为 "failing"
- 后续 Coding Agent 只能修改 `passes` 字段状态

```json
{
  "category": "functional",
  "description": "New chat button creates a fresh conversation",
  "steps": [
    "Navigate to main interface",
    "Click the 'New Chat' button",
    "Verify a new conversation is created"
  ],
  "passes": false
}
```

### Clean State（整洁状态）

每次会话结束时：
- 没有重大 bug
- 代码有序且有良好文档
- 开发者可以轻松开始新功能，无需先清理无关乱局

### 上下文桥接

通过 `claude-progress.txt` + git history：
- 让 Agent 快速理解工作状态
- 无需记住之前所有细节
- 灵感来自有效软件工程师的日常实践

## 性能验证

使用 Claude Agent SDK：
- 可处理数小时甚至数天的复杂任务
- 关键：多上下文窗口工作流
- 需要 initializer + coding 的不同 prompt

## 适用场景

✅ 适合多 Agent：
- 需要并行化的大量任务
- 信息超过单个上下文窗口
- 与众多复杂工具交互

❌ 不适合多 Agent：
- 需要所有 Agent 共享同一上下文
- Agent 之间有大量依赖

---

*来源: https://www.anthropic.com/engineering/effective-harnesses-for-long-running-agents*
