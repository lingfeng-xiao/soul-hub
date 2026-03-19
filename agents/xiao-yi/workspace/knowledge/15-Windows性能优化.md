# 系统优化 - Windows 性能提升

> 来源：小艺学习产出
> 标签：能力、系统优化、Windows

## 日常自检清单

| 项目 | 命令 | 阈值 |
|------|------|------|
| CPU | `Get-Process \| Sort CPU -Desc` | >90% 持续 |
| 内存 | `Get-Process \| Sort WS -Desc` | >80% |
| 磁盘 | `Get-PSDrive C` | <10GB |

## 快速优化

### 1. 清理临时文件
```powershell
Remove-Item $env:TEMP\* -Recurse -Force
```

### 2. 禁用启动项
```powershell
Get-CimInstance Win32_StartupCommand
```

### 3. 清理桌面
- 超过 30 天未访问的文件移动到归档

### 4. 磁盘清理
- 清理 Windows 更新缓存
- 清理回收站

## 进阶优化

- 关闭不必要的服务
- 调整虚拟内存
- 禁用视觉效果

---
*产出日期：2026-03-17*
