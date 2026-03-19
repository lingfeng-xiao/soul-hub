# skill: feishu-send-message

> 发送消息到飞书（当前用户）

## 触发条件

需要发送消息到飞书时使用。

## 使用方法

### 工具

使用 `message` 工具直接发送：

```powershell
message action=send channel=feishu message="消息内容"
```

### 参数说明

| 参数 | 必填 | 说明 |
|------|------|------|
| message | ✅ | 要发送的消息内容 |
| target | ❌ | 接收者（默认发给当前用户） |

### 示例

```powershell
# 发给当前用户
message action=send channel=feishu message="你好测试"

# 发给其他人
message action=send channel=feishu message="你好" target="ou_xxx"
```

## 注意事项

- 默认发送给当前用户（open_id: ou_8e027c4775516bfde0475378c5eff8f2）
- 消息内容为纯文本
