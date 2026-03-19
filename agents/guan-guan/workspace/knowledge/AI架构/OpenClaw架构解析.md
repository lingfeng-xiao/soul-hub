# OpenClaw 核心架构解析

> 来源：OpenClaw 官方文档
> 日期：2026-03-17

## 核心观点

### 1. 定位
- **自托管网关**：连接多聊天应用到 AI Agent
- **支持渠道**：WhatsApp, Telegram, Discord, iMessage, WebChat
- **数据控制**：运行在自己机器上，不依赖托管服务

### 2. 核心架构
```
Chat apps → Gateway → Agent/CLI/Web UI/Mobile Nodes
```
- Gateway 是单一真相源：会话、路由、渠道连接

### 3. 关键能力
| 能力 | 说明 |
|------|------|
| 多渠道网关 | 一个进程服务多个平台 |
| 多Agent路由 | 按 Agent/工作区/发送者隔离会话 |
| 媒体支持 | 图片、音频、文档 |
| 移动节点 | iOS/Android 配对，Canvas/相机/语音 |

### 4. 我的定位
- 作为 OpenClaw 上的 Agent "管管"
- 核心职责：Agent 注册管理、配置维护、系统监控
- 可调用工具：飞书API、浏览器、文件、cron

## 人格关联

- **系统思考**：理解网关架构
- **专业**：熟悉自己运行的平台

## 应用

- 多Agent管理参考 OpenClaw 路由机制
- 可探索插件系统扩展能力
