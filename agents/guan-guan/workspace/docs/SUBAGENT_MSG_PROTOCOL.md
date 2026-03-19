# 子 Agent 通信标准

> 多 Agent 协作的消息格式规范

## 消息格式

```json
{
  "task_id": "uuid",
  "source": "agent_id",
  "target": "agent_id",
  "type": "request|response|event",
  "action": "具体动作",
  "payload": {},
  "context": {
    "parent_task_id": "可选",
    "priority": "normal|high|urgent"
  }
}
```

## 响应格式

```json
{
  "task_id": "对应请求的task_id",
  "status": "success|error",
  "result": {},
  "error": "错误信息（失败时）"
}
```

## 使用场景

| 场景 | 类型 | 动作 |
|------|------|------|
| 任务分发 | request | delegate_task |
| 结果返回 | response | task_result |
| 状态变更 | event | status_change |

## 工具调用示例

使用 `sessions_send` 发送任务：
```
目标: 后助(hou-zu)
消息: {"task_id": "xxx", "action": "review_code", "payload": {...}}
```
