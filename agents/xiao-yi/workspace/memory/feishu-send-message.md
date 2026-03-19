# 飞书发消息配置

## 灵锋的飞书信息

- **open_id**: `ou_05181151d11790e64820d384321c74aa`
- **发送方式**: 使用 `feishu_im_user_message` (用户身份) 或 `message` 工具 (target: user:ou_xxx)

## 发送示例

```json
{
  "action": "send",
  "msg_type": "text",
  "content": "{\"text\":\"消息内容\"}",
  "receive_id": "ou_05181151d11790e64820d384321c74aa",
  "receive_id_type": "open_id"
}
```

## 注意事项

- 用户身份发送需要 OAuth 授权
- 机器人身份发送更稳定：用 `message` 工具，target 填 `user:ou_xxx`

> 记录于 2026-03-18
