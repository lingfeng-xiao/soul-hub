# 解决方案手册 (SOLUTIONS)

> 常见问题的标准化解决方案 - 必须先查这里！

---

## 壁纸问题

### 桌面壁纸太暗/黑色
**解决方案：**
```powershell
# 运行壁纸工作流脚本
powershell -ExecutionPolicy Bypass -File "C:\Users\16343\Downloads\set-wallpaper.ps1" -ImagePath "C:\Windows\Web\Wallpaper\Windows\img0.jpg"
```

### 锁屏壁纸是黑色
**解决方案：**
```powershell
# 启用 Windows 聚焦（每天自动换 Bing 壁纸）
Set-ItemProperty -Path 'HKCU:\Software\Microsoft\Windows\CurrentVersion\ContentDeliveryManager' -Name 'RotatingLockScreenEnabled' -Value 1 -Type DWord
```

### 壁纸工作流脚本位置
- 脚本：`C:\Users\16343\Downloads\set-wallpaper.ps1`
- 备用壁纸：`C:\Windows\Web\Wallpaper\Windows\img0.jpg` / `img19.jpg`

---

## 内存问题

### MCP 进程泄漏
**检测：**
```powershell
Get-Process | Where-Object {$_.Name -like "*chrome-devtools*" -or $_.Name -like "*mcp*"} | Select-Object Name, Id, @{N='Memory(MB)';E={[math]::Round($_.WorkingSet64/1MB,2)}}
```

**解决方案：**
```powershell
Get-Process | Where-Object {$_.Name -like "*chrome-devtools*"} | Stop-Process -Force
```

---

## 外部搜索

### Agent Reach (exa)
**命令：**
```powershell
mcporter call exa.web_search_exa --query "搜索内容"
```

---

*遇到问题，先查这里！*
