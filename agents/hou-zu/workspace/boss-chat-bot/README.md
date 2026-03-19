# Boss 直聘消息助手 🤖

自动获取 Boss 直聘消息并通过 OpenClaw AI 自动回复

## 功能特性

- 📬 获取聊天列表
- 💬 查看聊天详情
- ✨ AI 自动回复（接入 OpenClaw）
- 📝 手动发送消息
- 🔄 定时轮询新消息

## 快速开始

### 1. 安装依赖

```bash
cd boss-chat-bot
npm install
```

### 2. 扫码登录

```bash
npm run login
```

会出现浏览器窗口，请扫码登录 Boss 直聘。登录成功后会自动保存 Cookie。

### 3. 启动 API 服务

```bash
npm start
```

API 服务默认在 `http://localhost:3000` 运行。

### 4. 在 OpenClaw 中使用

通过对话发送以下命令：

- **查看消息** - 查看未读消息列表
- **查看和 XXX 的聊天** - 查看与某个 Boss 的聊天记录  
- **回复这个** - AI 自动回复最后一条未读消息
- **给 XXX 发送: 内容** - 手动发送消息

## API 接口文档

| 接口 | 方法 | 说明 |
|------|------|------|
| `/health` | GET | 健康检查 |
| `/api/login/status` | GET | 检查登录状态 |
| `/api/login` | POST | 扫码登录 |
| `/api/chat/list` | GET | 获取聊天列表 |
| `/api/chat/messages/:chatId` | GET | 获取聊天消息 |
| `/api/chat/send` | POST | 发送消息 |
| `/api/poll/start` | POST | 启动轮询 |
| `/api/poll/stop` | POST | 停止轮询 |

## 项目结构

```
boss-chat-bot/
├── src/
│   ├── browser.mjs          # Puppeteer 浏览器控制
│   ├── server.mjs            # API 服务
│   ├── handler.mjs           # 消息处理入口
│   ├── openclaw-integration.mjs  # OpenClaw 集成
│   └── login.mjs             # 登录脚本
├── data/                    # 数据目录（Cookie等）
├── package.json
└── README.md
```

## 与 OpenClaw 集成

### 方式 1: HTTP API 轮询

OpenClaw 定时调用 API 检查新消息：

```javascript
// 每分钟检查一次
setInterval(async () => {
  const response = await fetch('http://localhost:3000/api/chat/list')
  const { data } = await response.json()
  const unread = data.filter(chat => chat.unread)
  
  if (unread.length > 0) {
    // 通知 OpenClaw 生成回复
    // ...
  }
}, 60000)
```

### 方式 2: 回调模式

配置回调地址，有新消息时自动通知：

```bash
# 设置回调地址
export OPENCLAW_CALLBACK_URL=http://your-openclaw-url/callback
```

## 注意事项

⚠️ **风险提示**
- 使用此工具可能导致 Boss 直聘账号被风控
- 建议使用小号进行测试
- 合理控制发送频率，避免频繁操作

## 许可证

MIT
