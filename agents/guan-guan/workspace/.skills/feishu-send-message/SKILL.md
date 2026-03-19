# skill: feishu-send-message

> 发送消息到飞书

## 触发条件

需要发送消息到飞书时使用。

## 使用方法

### 工具

使用 `message` 工具：

```powershell
message action=send channel=feishu message="消息内容" target="ou_xxx"
```

### 用户 ID

当前用户 open_id：`ou_8e027c4775516bfde0475378c5eff8f2`

（从 MEMORY.md 读取）

### 参数说明

| 参数 | 必填 | 说明 |
|------|------|------|
| message | ✅ | 要发送的消息内容 |
| target | ✅ | 接收者 open_id 或 chat_id |

### 示例

```powershell
message action=send channel=feishu message="你好测试" target="ou_8e027c4775516bfde0475378c5eff8f2"
```

## 注意事项

- 必须先获得用户明确授权才能发送
- 消息内容需要是纯文本
- 发送前确认 target 正确
