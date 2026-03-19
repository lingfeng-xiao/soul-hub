# TOOLS.md - 管管的工具配置

> Agent 管理员日常工具配置、技巧与坑点记录

---

## 凭证位置

- `.credentials/` 目录（已 gitignore）
- 示例：`example-api.txt`

---

## OpenClaw 核心命令

### Gateway 管理

**状态：** ✅ 正常

**常用操作：**
```powershell
openclaw gateway status     # 查看网关状态
openclaw gateway start     # 启动网关
openclaw gateway stop      # 停止网关
openclaw gateway restart   # 重启网关
```

### uv (Python 环境)

**状态：** ✅ 正常

**配置：** ClawX 内置，已在 PATH

**常用操作：**
```bash
uv run python <script>     # 运行脚本
uv pip install <package>   # 安装包
```

---

## 飞书 API 工具

### 日历管理

**状态：** ✅ 正常

**工具：**
- `feishu_calendar_calendar` - 日历列表/主日历
- `feishu_calendar_event` - 日程 CRUD
- `feishu_calendar_freebusy` - 忙闲查询

**常用操作：**
```powershell
# 查询忙闲（安排会议前必查）
feishu_calendar_freebusy --time_min "2026-03-18T09:00:00+08:00" --time_max "2026-03-18T18:00:00+08:00" --user_ids ["ou_xxx"]

# 创建日程（必须加 user_open_id，否则用户看不到）
feishu_calendar_event create --user_open_id "ou_xxx" --summary "会议" --start_time "..." --end_time "..."
```

**坑点：**
- create 时必须传 `user_open_id`（消息上下文的 SenderId），否则日程只在应用日历上
- list 自动展开重复日程，但时间区间不能超过 40 天

---

### 任务管理

**状态：** ✅ 正常

**工具：**
- `feishu_task_task` - 任务 CRUD
- `feishu_task_tasklist` - 清单管理
- `feishu_task_comment` - 任务评论
- `feishu_task_subtask` - 子任务

**常用操作：**
```powershell
# 创建任务
feishu_task_task create --summary "任务标题" --members [{"id": "ou_xxx", "role": "assignee"}]

# 查询我的任务
feishu_task_task list
```

---

### 多维表格 (Bitable)

**状态：** ✅ 正常

**工具：**
- `feishu_bitable_app` - 表格应用
- `feishu_bitable_app_table` - 数据表
- `feishu_bitable_app_table_record` - 记录 CRUD
- `feishu_bitable_app_table_field` - 字段管理
- `feishu_bitable_app_table_view` - 视图管理

**字段类型：** 1=文本, 2=数字, 3=单选, 4=多选, 5=日期, 7=复选框, 11=人员, 15=超链接, 1001=创建时间, 1002=修改时间

**常用操作：**
```powershell
# 创建记录
feishu_bitable_app_table_record create --app_token "xxx" --table_id "xxx" --fields {"字段名": "值"}

# 批量创建（更高效）
feishu_bitable_app_table_record batch_create --records [{"fields": {...}}, {"fields": {...}}]
```

**坑点：**
- create 单条用 `fields`，批量用 `records`
- 超链接字段（type=15）必须完全省略 property 参数

---

### 文档管理

**状态：** ✅ 正常

**工具：**
- `feishu_create_doc` - 创建文档
- `feishu_fetch_doc` - 获取文档内容
- `feishu_update_doc` - 更新文档
- `feishu_doc_comments` - 评论管理

**常用操作：**
```powershell
# 从 Markdown 创建
feishu_create_doc --title "文档标题" --markdown "# 内容"

# 更新文档
feishu_update_doc --doc_id "xxx" --mode "append" --markdown "新增内容"
```

---

### 消息管理

**状态：** ⚠️ 需用户授权

**工具：**
- `feishu_im_user_message` - 发消息（需用户确认）
- `feishu_im_user_get_messages` - 获取历史消息
- `feishu_im_user_search_messages` - 搜索消息

**坑点：**
- `feishu_im_user_message` 以用户身份发消息，调用前必须确认发送对象和内容
- 机器人身份用 `feishu_im_bot_image` 下载图片

---

### 用户与群组

**状态：** ✅ 正常

**工具：**
- `feishu_get_user` - 获取用户信息
- `feishu_search_user` - 搜索员工
- `feishu_chat` - 群组管理
- `feishu_chat_members` - 群成员

---

### 云盘与知识库

**状态：** ✅ 正常

**工具：**
- `feishu_drive_file` - 云盘文件管理
- `feishu_wiki_space` - 知识空间
- `feishu_search_doc_wiki` - 文档搜索

---

## 浏览器控制

**状态：** ✅ 正常

**工具：** `browser`

**常用操作：**
```powershell
browser action="start"
browser action="snapshot"
browser action="act" --kind "click" --ref "e12"
```

---

## 技能 (Skills)

| 技能 | 用途 |
|------|------|
| `feishu-bitable` | 多维表格高级操作 |
| `feishu-calendar` | 日历与日程管理 |
| `feishu-im-read` | 消息读取与搜索 |
| `feishu-task` | 任务与清单管理 |
| `feishu-create-doc` | 创建飞书文档 |
| `feishu-fetch-doc` | 获取文档内容 |
| `feishu-update-doc` | 更新文档 |
| `node-connect` | 节点连接诊断 |
| `healthcheck` | 安全与健康检查 |
| `cron-manager` | 定时任务管理 |
| `clawdbot-logs` | 日志分析（ClawHub） |
| `database-admin` | 数据库管理（ClawHub） |

### ClawHub 技能安装

**状态：** ✅ 已安装 clawhub CLI

**安装命令：**
```bash
# 搜索技能
clawhub search <keyword>

# 安装技能（需先登录）
clawhub install <slug> --dir "C:\Users\16343\.openclaw\agents\guan-guan\workspace\.skills"

# 强制安装可疑技能
clawhub install <slug> --dir <path> --force
```

**已安装的 ClawHub 技能：**
- `clawdbot-logs` - 日志分析
- `database-admin` - 数据库管理

**注意：** 技能安装后需要重启 Gateway 才能生效

---

## 坑点汇总

1. **飞书 OAuth 过期**：返回 `token_expired` 时，调用 `feishu_oauth revoke` 撤销授权，系统会自动重新发起
2. **日程创建心跳户不可见**：必须传 `user_open_id` 参数
3. **多维表格批量操作**：用 `batch_create`/`batch_update`，单条操作效率低
4. **消息发送**：必须先确认发送对象和内容，不可自行发送
5. **超链接字段**：创建时完全省略 `property` 参数
6. **Cron 任务 rate_limit**：进化任务太频繁可能触发 API 限流，考虑增加进化间隔或优化任务执行时间
6. **进化时主动检索工具**：遇到知识搜索需求时，**先检索 TOOLS.md/SKILLS 确认已有工具**，再考虑外部 API

---

## Agent Reach（互联网访问增强）

**状态：** ✅ 已安装 (2026-03-19)
**位置：** `C:\Users\16343\.agent-reach-venv\`

**用途**：增强互联网访问能力（YouTube、V2EX、Twitter、小红书等）

**已安装渠道 (3/15)**：
- ✅ V2EX 节点与主题
- ✅ RSS/Atom 订阅源
- ✅ 任意网页（Jina Reader）

**可配置渠道**：
- Twitter (xreach CLI - 可选)
- 小红书 (需 Docker)
- 微博 (需 pip install mcp-server-weibo)
- YouTube (需 yt-dlp)
- 小宇宙播客 (需 ffmpeg + Groq API)

**使用命令**：
```powershell
# 激活环境
C:\Users\16343\.agent-reach-venv\Scripts\agent-reach.exe

# 检查状态
agent-reach doctor

# 配置 Groq API (播客转文字)
agent-reach configure groq-key gsk_xxxxx
```

---

*持续更新，记录实际使用中的坑和解决方案*

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
