# MCP (Model Context Protocol) 深度指南

> MCP - AI 应用的 USB-C 接口 | 2026-03-18

## 什么是 MCP

**Model Context Protocol (MCP)** 是一个开放标准，用于将 AI 应用连接到外部系统。

把它想象成 AI 应用的 **USB-C 端口**：就像 USB-C 提供连接电子设备的标准化方式，MCP 提供了将 AI 应用连接到外部系统的标准化方式。

## MCP 能做什么

- AI 可以访问你的 Google Calendar 和 Notion，成为更个性化的助手
- Claude Code 可以根据 Figma 设计生成整个 Web 应用
- 企业聊天机器人可以连接组织内的多个数据库
- AI 模型可以直接在 Blender 中创建 3D 设计并打印

## 生态系统支持

MCP 是开放协议，被广泛支持：
- **AI 助手**: Claude, ChatGPT
- **开发工具**: VS Code, Cursor, Zed
- **更多客户端**: 详见官方市场

## 传输方式

| 传输方式 | 执行环境 | 部署 | 用户数 | 输入 | 认证 |
|---------|---------|------|-------|------|-----|
| **stdio** | 本地 | Cursor 管理 | 单用户 | Shell 命令 | 手动 |
| **SSE** | 本地/远程 | 部署为服务 | 多用户 | SSE 端点 URL | OAuth |
| **Streamable HTTP** | 本地/远程 | 部署为服务 | 多用户 | HTTP 端点 URL | OAuth |

## 协议能力

| 特性 | 支持 | 说明 |
|-----|------|-----|
| **Tools** | ✅ | AI 模型可执行的函数 |
| **Prompts** | ✅ | 模板化消息和工作流 |
| **Resources** | ✅ | 可读取引用的结构化数据源 |
| **Roots** | ✅ | 服务端查询 URI 或文件系统边界 |
| **Elicitation** | ✅ | 服务端请求用户额外信息 |
| **Apps** | ✅ | MCP 工具返回的交互式 UI |

## 在 Cursor 中配置

```json
{
  "mcpServers": {
    "server-name": {
      "command": "npx",
      "args": ["-y", "mcp-server"],
      "env": {
        "API_KEY": "value"
      }
    }
  }
}
```

### STDIO 服务器配置字段

| 字段 | 必需 | 说明 |
|-----|------|-----|
| **type** | 是 | 连接类型，填 `"stdio"` |
| **command** | 是 | 启动服务器的命令 |
| **args** | 否 | 传给命令的参数数组 |
| **env** | 否 | 服务器的环境变量 |
| **envFile** | 否 | 环境变量文件路径 |

## 为什么 MCP 重要

- **开发者**: 减少构建/集成 AI 应用的时间和复杂性
- **AI 应用**: 访问数据源、工具和应用生态系统，增强能力
- **终端用户**: 获得更强大的 AI 应用，可以访问数据并代为执行任务

## MCP Apps

MCP 工具可以返回交互式 UI 以及标准工具输出。遵循渐进增强，如果主机无法渲染 app UI，相同工具仍可通过普通 MCP 响应工作。

---

*来源: modelcontextprotocol.io, cursor.sh*
