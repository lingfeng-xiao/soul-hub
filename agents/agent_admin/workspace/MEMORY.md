# 记忆库

## Agent 命名规范

→ 详见: `C:\Users\16343\.openclaw\agents\NAMING_CONVENTION.md`

包括：ID 规则、名称规则、关键词规则、角色定义模板

## Agent 创建工作流

→ 详见: `AGENT_CREATE_WORKFLOW.md`

### 快速摘要

1. **命名**: 遵守 NAMING_CONVENTION.md 规范
2. **创建目录**: `C:\Users\16343\.openclaw\agents\{id}\`
3. **配置5个核心文件**: AGENTS.md, IDENTITY.md, SOUL.md, USER.md, TOOLS.md
4. **注册**: 修改 openclaw.json 添加到 agents.list
5. **配置触发**: 在 channels.*.routing.rules 添加关键词
6. **应用**: gateway config.apply
7. **更新注册表**: 更新 `REGISTRY.md`

### 核心文件

| 文件 | 作用 |
|------|------|
| NAMING_CONVENTION.md | 命名规范 |
| REGISTRY.md | Agent 注册表 |
| AGENTS.md | Agent 元信息 |
| IDENTITY.md | 身份定义 |
| SOUL.md | 性格/系统提示词 |
| USER.md | 用户信息 |
| TOOLS.md | 工具配置 |

### 已创建 Agent（按新名称）

| ID | 名称 | 用途 | 创建时间 |
|----|------|------|----------|
| xiao-yi | 小艺（电脑管家） | UI设计师电脑管家 | 2026-03-17 |
| mian-xiao-zhu | 面小助（面试辅导） | 面试辅导 | 2026-03-17 |
| guan-guan | 管管（管理员） | Agent管理员 | 2026-03-17 |
