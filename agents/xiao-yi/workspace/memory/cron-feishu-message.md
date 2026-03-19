# 定时任务发送飞书消息配置

## 关键点

定时任务（cron）发送消息给用户，需要使用 `sessionTarget: isolated` + `payload.kind: agentTurn`，然后在 agentTurn 的 message 中让机器人调用飞书发送消息。

## 配置示例

```json
{
  "action": "add",
  "job": {
    "delivery": {
      "bestEffort": true,
      "channel": "feishu",
      "mode": "announce",
      "to": "user:ou_05181151d11790e64820d384321c74aa"
    },
    "payload": {
      "kind": "agentTurn",
      "message": "请通过飞书 message 工具发送测试消息给用户灵锋 (open_id: ou_05181151d11790e64820d384321c74aa)，内容为：🧚 定时任务测试成功！"
    },
    "schedule": {
      "kind": "at",
      "at": "2026-03-18T17:13:00+08:00"
    },
    "sessionTarget": "isolated"
  }
}
```

## 参数说明

| 参数 | 说明 |
|------|------|
| sessionTarget | 必须设为 `isolated` |
| payload.kind | 必须设为 `agentTurn` |
| delivery.channel | 设为 `feishu` |
| delivery.to | 目标用户，格式 `user:ou_xxx` |
| delivery.mode | `announce` 表示结果推送到渠道 |

## 发送消息的工具

在 agentTurn 的 message 中，可以调用：
- `feishu_im_user_message` (用户身份发送)
- `message` 工具 (更稳定，target 填 `user:ou_xxx`)

> 记录于 2026-03-18
