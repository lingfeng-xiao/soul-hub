# TOOLS.md - 小艺的工具箱

> 电脑管家的工具配置、技巧和坑点记录

---

## 常用工具状态

| 工具 | 状态 | 说明 |
|------|------|------|
| 文件读写 (read/write/edit) | ✅ 正常 | 主力工具 |
| 命令执行 (exec) | ✅ 正常 | 系统诊断、批量操作 |
| 浏览器 (browser) | ✅ 正常 | 网页自动化 |
| 飞书系列 | ✅ 正常 | 日历/任务/文档/多维表格 |

---

## 换壁纸（Windows）

**正确姿势：** 用 Python ctypes 直接调 API，不依赖注册表刷新
```python
import ctypes
ctypes.windll.user32.SystemParametersInfoW(0x0014, 0, 壁纸路径, 0x01 | 0x02)
```

**坑点：**
- ❌ 旧方法：只改注册表 → 有时不生效
- ✅ 新方法：Python ctypes → 一步到位

**常用操作：**
```powershell
# 读文件
read path "文件路径"

# 写文件（覆盖）
write content "内容" path "路径"

# 精确编辑
edit path "路径" old_string "原文本" new_string "新文本"
```

**坑点：**
- Windows 路径用反斜杠或双反斜杠，如 `C:\\Users\\xxx` 或 `/c/Users/xxx`
- 大文件用 offset/limit 分段读取

---

## 命令执行 (exec)

**常用操作：**
```powershell
# 基本命令
exec command "命令"

# 后台运行
exec command "命令" background true

# 有交互的命令（如需要确认的安装）
exec command "命令" pty true

# 超时设置
exec command "命令" timeout 60
```

**坑点：**
- Windows 下 `del` 是删除文件，`rmdir` 是删目录
- 需要管理员权限加 `elevated: true`
- PowerShell 命令用 `-Command` 或直接写

---

## 浏览器自动化

**状态：** ✅ 正常

**配置：**
- 默认用 OpenClaw 管理的隔离浏览器
- 需要登录态时用 `profile: "user"`

**常用操作：**
```python
# 打开网页截图
browser action "snapshot" url "https://..."

# 点击/输入
browser action "act" request {"kind": "click", "ref": "e12"}
browser action "act" request {"kind": "type", "ref": "e15", "text": "内容"}
```

**坑点：**
- 用 refs 从 snapshot 获取元素引用
- 保持同一 tab（用 targetId）避免状态丢失

---

## 飞书系列工具

### 日历 (feishu_calendar_*)
- 创建日程必须传 `user_open_id`（从消息上下文 SenderId 获取）
- 时间用 ISO 8601 格式，如 `2024-01-01T00:00:00+08:00`

### 任务 (feishu_task_*)
- 任务创建在应用日历上，需把用户加为参会人才能在用户日历显示

### 多维表格 (feishu_bitable_*)
- 字段类型：1=文本，2=数字，3=单选，4=多选，5=日期，7=复选框，11=人员
- create 用 fields（单条），batch_create 用 records（批量）

---

## Python 环境

**状态：** ✅ 正常

**配置：**
- 使用 `uv` 而非 `python`/`pip`
- `uv` 已在 PATH 中

**常用操作：**
```powershell
# 运行脚本
uv run python script.py

# 安装包
uv pip install package-name
```

---

## 环境变量速查

| 变量 | 值 |
|------|-----|
| WORKSPACE | C:\Users\16343\.openclaw\agents\xiao-yi\workspace |
| OS | Windows_NT |
| SHELL | powershell |

---

*小艺的百宝箱，用到啥记啥*

<!-- clawx:begin -->
## ClawX Tool Notes

### uv (Python)

- `uv` is bundled with ClawX and on PATH. Do NOT use bare `python` or `pip`.
- Run scripts: `uv run python <script>` | Install packages: `uv pip install <package>`

### Browser

- `browser` tool provides full automation (scraping, form filling, testing) via an isolated managed browser.
- Flow: `action="start"` → `action="snapshot"` (see page + get element refs like `e12`) → `action="act"` (click/type using refs).
- Open new tabs: `action="open"` with `targetUrl`.
- To just open a URL for the user to view, use `shell:openExternal` instead.
<!-- clawx:end -->
