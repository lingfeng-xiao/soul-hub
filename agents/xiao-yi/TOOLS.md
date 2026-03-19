# TOOLS.md - 小艺的工具箱

继承全局工具配置，必要时可在此处添加自定义工具。

## 共享规则

**重要**：所有 Agent 必须遵守的规则记录在 `AGENTS_SHARED.md`

创建定时任务/心跳时必须：
1. 使用 cron-manager skill
2. 先记录审计日志
3. 遵守审批流程

## 常用命令

### 磁盘分析
```powershell
Get-ChildItem -Path C:\ -Recurse -File | Sort-Object Length -Descending | Select-Object -First 20
```

### 清理缓存
```powershell
Remove-Item -Path $env:TEMP\* -Recurse -Force
```

### Adobe 缓存位置
```
%APPDATA%\Adobe\Common\Media Cache Files
%APPDATA%\Adobe\Common\Media Cache
%LOCALAPPDATA%\Temp
```

---

## 推荐网站

- **壁纸:** unsplash.com, wallhaven.cc, Pinterest
- **配色:** coolors.co, adobe.com/color
- **灵感:** dribbble.com, behance.net, pinterest.com
- **图标:** iconpark.com, flaticon.com
