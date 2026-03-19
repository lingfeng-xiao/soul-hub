# Agent 创建标准工作流

> 用于快速创建独立 Agent 的标准化流程

> ⚠️ **命名必须遵守**: `C:\Users\16343\.openclaw\agents\NAMING_CONVENTION.md`

---

## 流程概览

```
┌─────────────┐    ┌─────────────┐    ┌─────────────┐    ┌─────────────┐
│  1. 命名    │ -> │  2. 创建    │ -> │  3. 配置    │ -> │  4. 注册    │
│  Agent名称   │    │  目录结构   │    │  核心文件   │    │  到配置     │
└─────────────┘    └─────────────┘    └─────────────┘    └─────────────┘
       │                                                               │
       │                    ┌─────────────┐    ┌─────────────┐         │
       └──────────────────> │  5. 更新    │ -> │  6. 完成    │         │
                            │  注册表     │    │             │         │
                            └─────────────┘    └─────────────┘
```

---

## 详细步骤

### 步骤 1: 命名 Agent（必须遵守规范）

> 📋 详细规则见 `NAMING_CONVENTION.md`

**确定信息：**
- [ ] Agent ID（kebab-case，如 `xiao-yi`）
- [ ] Agent 名称（中文人格化，如 "小艺"）
- [ ] Emoji 图标（如 🎨）
- [ ] 风格定位（专业型/亲切型/创意型/简洁型）

**命名检查：**
- [ ] ID 无重复（查 REGISTRY.md）
- [ ] 名称无重复
- [ ] 符合人格化原则

---

### 步骤 2: 创建目录结构

```powershell
# PowerShell 命令
New-Item -ItemType Directory -Force -Path 'C:\Users\16343\.openclaw\agents\{agent-id}\workspace'
```

**⚠️ 重要：只需创建 workspace 目录，定制文件全部放在 workspace 下**

| 文件 | 位置 | 作用 |
|------|------|------|
| `AGENTS.md` | workspace/ | Agent 元信息 |
| `IDENTITY.md` | workspace/ | 身份定义 |
| `SOUL.md` | workspace/ | 性格/系统提示词 |
| `USER.md` | workspace/ | 用户信息 |
| `TOOLS.md` | workspace/ | 工具配置 |
| `MEMORY.md` | workspace/ | 记忆库（可选） |

**目录结构：**
```
{agent-id}/
└── workspace/          ← 所有定制文件放这里
    ├── AGENTS.md
    ├── IDENTITY.md
    ├── SOUL.md
    ├── TOOLS.md
    ├── USER.md
    └── MEMORY.md      （可选）
```

---

### 步骤 3: 配置核心文件

> ⚠️ **所有文件必须放在 workspace 目录下！**

#### 3.1 AGENTS.md（模板）

```markdown
# AGENTS.md - {Agent名称}

## Agent 信息

- **ID:** {agent-id}
- **名称:** {名称}
- **用途:** {用途描述}

## 触发关键词

- {关键词1}
- {关键词2}
```

#### 3.2 IDENTITY.md（模板）

```markdown
# IDENTITY.md - {名称}身份卡

- **Name:** {名称} ({英文名})
- **Creature:** AI Agent
- **Emoji:** {emoji}
- **Avatar:** {描述}
- **Vibe:** {风格描述}

---

## 特点

- {特点1}
- {特点2}
- {特点3}
```

#### 3.3 SOUL.md（模板）

```markdown
# SOUL.md - {名称}是谁

_{一句话描述核心性格}_

## 核心性格

**特点1** — {描述}
**特点2** — {描述}

---

## 服务范围

### {服务分类1}
- {具体功能}

### {服务分类2}
- {具体功能}

---

## 响应风格

- **风格描述**：{描述}
- **emoji使用**：{emoji列表}

---

## 边界

- {边界1}
- {边界2}
```

#### 3.4 USER.md（模板）

```markdown
# USER.md - 关于你

_正在了解中..._

- **Name:** 
- **偏好:** 正在了解...

---

有什么偏好可以告诉我，我会记住的！
```

#### 3.5 TOOLS.md（模板）

```markdown
# TOOLS.md - {名称}的工具箱

继承全局工具配置，必要时可在此处添加自定义工具。

## 常用命令

### {场景}
```{language}
# 命令
```

---

## 推荐资源

- **{类别}:** {网站/工具}
```

---

### 步骤 4: 注册到配置

#### 4.1 添加到 agents.list

在 `openclaw.json` 的 `agents.list` 数组中添加：

```json
{
  "id": "{agent-id}",
  "name": "{名称}",
  "workspace": "C:\\\\Users\\\\16343\\\\.openclaw\\\\agents\\\\{agent-id}\\\\workspace",
  "agentDir": "C:\\\\Users\\\\16343\\\\.openclaw\\\\agents\\\\{agent-id}",
  "identity": {
    "name": "{名称}",
    "emoji": "{emoji}",
    "theme": "{主题}"
  }
}
```

#### 4.2 配置触发规则（可选）

在 `channels.feishu.routing.rules` 添加：

```json
{
  "name": "{名称}",
  "agent": "{agent-id}",
  "when": {
    "message.contains": [
      "关键词1",
      "关键词2"
    ]
  }
}
```

#### 4.3 应用配置

使用 `gateway config.apply` 重启网关

---

### 步骤 5: 更新注册表

> 文件位置: `C:\Users\16343\.openclaw\agents\REGISTRY.md`

> ⚠️ **重要：工作目录统一写 `workspace/`，无需写全路径**

添加新 Agent 到表格：

```markdown
| ID | 名称 | 用途 | 触发关键词 | 状态 |
|----|------|------|------------|------|
| {agent-id} | {名称} | {用途} | {关键词} | ✅ |
```

---

## 检查清单

### 可选增强

1. **快捷命令**：添加常用操作到 TOOLS.md
2. **自动触发**：配置飞书/微信等渠道关键词
3. **子权限**：配置 subagents.allowAgents
4. **沙箱**：根据需要配置 sandbox 模式

### 检查清单

创建完成后确认：
- [ ] 所有文件已创建
- [ ] ID 唯一不冲突
- [ ] 配置已应用
- [ ] 触发关键词已设置
- [ ] 注册表 REGISTRY.md 已更新
- [ ] 可以正常召唤

---

## 常见问题

**Q: 如何测试新 Agent？**
A: 通过飞书发送关键词，或在当前会话中使用 sessions_spawn

**Q: 修改配置需要重启吗？**
A: 是的，需要 config.apply 触发重启

**Q: 如何删除 Agent？**
A: 从 config 中移除 entry，然后删除 agent 目录

---

## ⚠️ 常见错误与避免

### 错误1: 在根目录创建文件

**错误做法：**
```
{agent-id}/
├── AGENTS.md      ❌ 错误位置
├── IDENTITY.md    ❌ 错误位置
└── workspace/
    └── ...        ✅ 正确位置
```

**正确做法：**
```
{agent-id}/
└── workspace/     ✅ 所有文件放这里
    ├── AGENTS.md
    ├── IDENTITY.md
    ├── SOUL.md
    ├── TOOLS.md
    └── USER.md
```

### 错误2: 配置文件路径与实际不符

**常见问题：**
- openclaw.json 中 workspace 指向不存在目录
- 只创建了根目录文件，没有创建 workspace 目录

**避免方法：**
- 创建前先确认目录结构
- 配置完成后用 `openclaw status --all` 检查

### 错误3: 旧 Agent 配置未清理

**常见问题：**
- 重命名 Agent 后旧 ID 仍在配置中
- 多个配置指向同一目录

**避免方法：**
- 每次配置用 `config.apply` 完整替换
- 清理不再使用的旧配置项

---

*最后更新: 2026-03-17*
