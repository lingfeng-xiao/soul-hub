# skill: cron-manager

> 定时任务创建与管理

## 触发条件

需要创建、管理定时任务时使用。

## 使用方法

### 创建定时任务（完整流程）

**必须按顺序执行以下步骤**：

```
# 1. 记录审计日志（必须）
Append to file: knowledge/cron-audit.md
内容:
## [时间] [申请人] 创建 [任务名] ([ID])
- 申请人: xxx
- 操作: 创建
- 任务名: xxx
- 频率: xxx
- 审批状态: 已通过

# 2. 创建任务
openclaw cron create --name "任务名" --cron "频率" --message "内容" --timeout-seconds X --announce --channel feishu --to "群ID"

# 3. 发送上线通知（必须）
message action=send channel=feishu message="✅ 已上线: [任务名] ([ID])" target="群ID"

# 4. 清理临时会话（必须）
subagents list  # 查看当前会话
# 如果有工作流产生的临时会话，使用 subagents kill 清理
```

### 审计日志格式

```
## 2026-03-18 21:00 小艺 创建 最终测试 (09460c9e)
- 申请人: 小艺
- 任务名: 最终测试
- 频率: */30 * * * *
- 审批状态: 已通过
```

### 查询任务

```
openclaw cron list
```

### 删除任务

```
openclaw cron remove --id [任务ID]
```

## 完整流程检查清单

- [ ] 1. 记录审计日志
- [ ] 2. 创建 cron 任务
- [ ] 3. 发送上线通知
- [ ] 4. 清理临时会话
- [ ] 5. 删除测试任务（测试完成后）

## 注意事项

- 创建任务前必须先记录审计日志
- 创建后必须发送上线通知
- 创建后必须清理临时会话
- 测试任务完成后必须删除
- 审计日志位置: knowledge/cron-audit.md
