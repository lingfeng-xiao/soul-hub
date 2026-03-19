# Windows 自动化最佳实践

## 核心要点

### 1. PowerShell 是 Windows 自动化的基石
- 大多数系统操作可通过 PowerShell 完成
- 使用 `exec` 工具执行命令
- 示例：`Get-Process`, `Get-Service`, `Get-EventLog`

### 2. Python 在 Windows 的最佳实践
- 使用 `uv` 代替原生 `python`（ClawX 环境）
- 命令：`uv run python <script>`
- 包管理：`uv pip install <package>`

### 3. 浏览器自动化
- 使用 `browser` 工具进行 UI 自动化
- 流程：`start` → `snapshot` → `act`
- 适用于网页表单、测试、爬取

### 4. 文件操作
- Windows 路径使用反斜杠 `\` 或正斜杠 `/`
- PowerShell 处理路径更安全
- 注意：长路径需要 `\\?\` 前缀

### 5. 定时任务
- Windows Task Scheduler 用于持久定时任务
- Cron 用于 Agent 级别调度
- 推荐：业务用 Cron，系统用 Task Scheduler

## 避坑指南
- ❌ 避免硬编码用户目录，使用 `$env:USERPROFILE`
- ❌ 避免空格路径未加引号
- ✅ 总是使用绝对路径
- ✅ 日志输出到文件，便于排查

## 应用场景
1. 定时备份文件
2. 监控系统状态
3. 自动化部署
4. 批量处理数据
