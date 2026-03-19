# Windows 自动化最佳实践

> 第21次进化 - 2026-03-19

## 核心要点

### 1. 计划任务 (Task Scheduler)
```powershell
# 创建每日执行的任务
schtasks /create /tn "OpenClawDaily" /tr "powershell -File C:\scripts\daily.ps1" /sc daily /st 09:00
```

### 2. PowerShell 脚本最佳实践
- 使用 `-ExecutionPolicy Bypass` 避免执行策略限制
- 错误处理：`try/catch` + `$ErrorActionPreference`
- 日志输出：使用 `Write-Host` 或写入日志文件

### 3. OpenClaw Cron 定时任务
- 配置文件：`openclaw gateway config.get`
- 添加任务：`cron add`
- 查看任务：`cron list`
- 任务类型：`systemEvent`（主会话）或 `agentTurn`（隔离会话）

### 4. Windows 服务监控
```powershell
# 检查服务状态
Get-Service -Name "OpenClawGateway"
```

## 应用场景

| 场景 | 方案 | 优先级 |
|------|------|--------|
| 定时健康检查 | cron job + healthcheck skill | 高 |
| 日志清理 | PowerShell 脚本 + 计划任务 | 中 |
| Agent 状态监控 | cron + 脚本检查 | 高 |

## 避坑指南

1. **时区问题**：Windows 计划任务默认本地时区，OpenClaw cron 使用 UTC
2. **权限问题**：创建计划任务需要管理员权限
3. **路径空格**：脚本路径用双引号包裹

---

**价值分**: 7/10 - 实用性强，与管管运行环境匹配
