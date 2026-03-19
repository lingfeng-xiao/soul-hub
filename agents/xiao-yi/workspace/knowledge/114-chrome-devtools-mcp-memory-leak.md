# Chrome DevTools MCP 进程泄漏问题分析与自动清理方案

## 问题描述

Chrome DevTools MCP (chrome-devtools-mcp) 是一个用于浏览器自动化的工具，但在使用过程中会出现进程泄漏问题，导致大量内存被占用。

## 症状

- 多个 node.exe 进程，每个占用 500MB-1GB 内存
- 4个主要进程即可占用 3-4GB 内存
- 内存使用率从正常水平飙升到 90%+

## 根因分析

1. **MCP 服务进程泄漏**：chrome-devtools-mcp 在每次浏览器自动化后没有正确退出
2. **累积效应**：每次使用浏览器自动化都会启动新进程，旧进程不退出
3. **隐蔽性**：进程看起来是"正常"的 node 进程，用户难以识别

## 解决方案

### 1. 手动清理（当前方案）

```powershell
# 查找高内存chrome-devtools-mcp进程
Get-Process -Name node | Where-Object { 
    $cmd = (Get-CimInstance Win32_Process -Filter "ProcessId=$($_.Id)").CommandLine
    $cmd -match "chrome-devtools-mcp" -and $_.WorkingSet64 -gt 100MB
}
# 清理这些进程
Stop-Process -Id <PID> -Force
```

### 2. 自动感知清理（推荐）

在 perception.py 中添加自动检测和清理逻辑：

```python
def detect_chrome_mcp_leak():
    """检测chrome-devtools-mcp进程泄漏"""
    # 查找chrome-devtools-mcp进程
    # 如果>2个或总内存>1GB，自动清理
```

### 3. 预防措施

- 使用完浏览器自动化后，确保进程正确退出
- 定期重启 ClawX 或清理后台进程

## 感知强化方向

将这个知识转化为感知能力：
- **进程画像感知**：不只是检测内存使用率，还要识别问题进程类型
- **自动清理触发**：检测到泄漏模式时自动执行清理
- **历史趋势**：记录MCP进程数量历史，预测泄漏趋势

## 产出时间

2026-03-19 14:25
