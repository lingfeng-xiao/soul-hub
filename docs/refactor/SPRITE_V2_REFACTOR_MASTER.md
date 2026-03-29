# Sprite V2 重构总设计

版本：`v1.0`  
最后更新：`2026-03-29`  
主仓库：`sprite-be`  
适用范围：`Sprite 自用版 / 数字生命主链 / 本地开发 + jd 服务器集成`

## 1. 文档目的

这份文档用于把 Sprite 当前已经形成的生命闭环、仍然并存的旧系统、部署与 Git 基线问题，以及后续并行重构的目标边界一次性固定下来。  
它不是产品介绍，也不是开发日志，而是后续所有 worker agent 和人工集成的唯一设计基线。

本轮重构的目标不是把 Sprite 收缩成纯工具，而是把它收敛成一个真正可日常使用的数字生命：

- 有稳定身份
- 有连续记忆
- 有生命快照
- 有可控自治
- 能帮助学习与工作
- 能持续自我演化

## 2. 当前状态快照

以下事实均基于 `2026-03-29` 的本地仓库与运行探针：

### 2.1 已成立的生命主链

- 前端主路由已经收敛为 `/`、`/chat`、`/memory`、`/settings`
- 后端本地可启动，`/actuator/health` 返回 `UP`
- 下列接口已经可返回：
  - `GET /api/life/snapshot`
  - `GET /api/life/autonomy/status`
  - `GET /api/life/journal`
  - `GET /api/model/config`
- 新主链后端入口已存在：
  - `LifeController` 负责 `/api/life/*`
  - `ModelController` 负责 `/api/model/*`
- 新主链应用服务已存在：
  - `com.lingfeng.sprite.life.*`
- 新主链领域服务已存在：
  - `com.lingfeng.sprite.domain.identity`
  - `com.lingfeng.sprite.domain.self`
  - `com.lingfeng.sprite.domain.relationship`
  - `com.lingfeng.sprite.domain.goal`
  - `com.lingfeng.sprite.domain.command`
  - `com.lingfeng.sprite.domain.snapshot`

### 2.2 仍并存的旧系统

- 旧前端页面、旧 hooks、旧 stores、旧 API 模块仍在仓库中
- 旧后端主链仍然存在：
  - `SpriteController`
  - `MobileApiController`
  - `SpriteService`
  - `service.ActionExecutor`
  - WebSocket / Chat 旧通道
- 新命令主链已经成形，但 `CommandOrchestrator` 仍带有 placeholder 性质，尚未成为唯一真实执行内核
- `/memory` 页面仍未完全接入新的生命主链心智模型

### 2.3 数据与部署现状

- 本地默认数据源仍是 `H2 file mode`
- `application.yml` 当前同时启用了：
  - `ddl-auto: update`
  - `sql.init.mode: always`
- 仓库已有 MySQL compose 配置，但本地默认并不走 MySQL
- 仓库同时存在：
  - `schema.sql`
  - `db/migration/V1__life_runtime.sql`
- 当前 `pom.xml` 中没有启用 Flyway 作为正式 schema authority

### 2.4 Git 与服务器现状

- 本地 `sprite-be` 与 `sprite-fe` 的原始 `.git` 目录存在 ACL 问题
- 当前阻塞表现：
  - 无法在原仓库直接执行 `git checkout -b codex/...`
  - 根因是 `.git` 及其 `refs/logs` 路径存在显式/继承 `DENY` ACL
- `jd` 服务器具备：
  - Git
  - Docker
  - Docker Compose
  - Java 21
- `jd` 上旧目录 `~/sprite`、`~/sprite-fe` 不是 Git worktree，不再作为正式集成目录
- 已建立新的服务器标准目录：
  - `~/worktrees/sprite-be`
  - `~/worktrees/sprite-fe`
- 两个服务器仓库都已切到：
  - `codex/refactor-masterplan`

## 3. 重构目标与非目标

### 3.1 重构目标

- 把前台产品面固定为 4 个入口：生命主页、对话/命令、记忆、设置
- 把后端 owner-facing 主链固定到 `/api/life` 与 `/api/model`
- 把生命状态与运行日志稳定落到结构化持久层
- 把自治能力做成“高自治但有边界”的默认模式
- 把部署标准统一为：
  - `local-h2` 用于本地 smoke/test
  - `prod-mysql + flyway` 用于服务器正式运行
- 把后续并行执行切成可独立交付的任务包，降低上下文爆炸风险

### 3.2 非目标

- 本轮不做多人协作、多租户、开放平台化
- 本轮不把生命域拆成高度范式化的复杂数据库模型
- 本轮不彻底删除所有旧代码
- 本轮不做一次性全仓大搬家
- 本轮不把数字生命退化成纯效率工具

## 4. 目标架构

### 4.1 总体分层

Sprite V2 固定采用四层结构：

1. 前台产品层  
面向你自己的生命体验，只有 4 个主入口。

2. 主链接口层  
对外只暴露生命相关状态、命令、自治、模型配置。

3. 生命领域层  
身份、自我、关系、目标、命令编排、快照生成、自治策略。

4. 持久化与部署层  
状态表 + 事件表、环境配置、Compose、服务器工作目录、验收流程。

### 4.2 前端边界

主产品面固定为：

- `/`：生命主页
- `/chat`：对话/命令面板
- `/memory`：生命记忆
- `/settings`：配置与自治控制

主链前端只允许依赖：

- `GET /api/life/snapshot`
- `GET /api/life/journal`
- `GET /api/life/autonomy/status`
- `POST /api/life/commands`
- `POST /api/life/autonomy/pause`
- `POST /api/life/autonomy/resume`
- `POST /api/life/reset`
- `GET /api/model/config`
- `PUT /api/model/config`
- `POST /api/model/test`

以下前端模块从主链冻结：

- dashboard 相关页面与 hooks
- mcp 相关页面与 hooks
- skill 相关页面与 hooks
- team / agent / websocket 旧链路
- 不再服务主链的旧 stores

`/memory` 属于主链，但允许第一波作为“待主链化子系统”保留局部独立改造，不要求与 `/` 和 `/chat` 同一次完全重写。

### 4.3 后端边界

后端新的 canonical owner-facing 主链固定为：

- `LifeController`
- `ModelController`
- `com.lingfeng.sprite.life.*`
- `com.lingfeng.sprite.domain.*`

兼容层固定为：

- `SpriteController`
- `MobileApiController`
- `SkillController`
- `McpController`
- `AgentController`
- 旧 WebSocket / chat / sprite runtime 路径

兼容层保留，但冻结：

- 不再承载新产品语义
- 不再成为前端主链依赖
- 只在必要时作为 adapter 或调试入口使用

### 4.4 数据层边界

本轮采用“状态表 + 事件表”的混合方案。

状态表保留：

- `life_runtime_state`
- `runtime_model_config`
- `autonomy_policy`

事件表保留：

- `life_journal_entries`
- `life_command_executions`

本轮不新增大量细粒度业务表来拆分 identity / self / relationship / goals。  
这些状态仍允许以聚合 JSON 方式保留在 `life_runtime_state` 中。

### 4.5 部署层边界

环境边界固定如下：

- `local-h2`
  - 用途：本地 smoke、快速测试、临时联调
  - 不作为正式部署权威环境
- `prod-mysql`
  - 用途：`jd` 服务器常驻环境
  - 必须配套 Flyway
  - 必须是唯一生产 schema authority

`schema.sql` 视为当前过渡产物。  
当 `prod-mysql + flyway` 完整恢复后，生产环境不再依赖 `schema.sql` 作为权威建表来源。

## 5. 固定接口契约

### 5.1 生命读取接口

`GET /api/life/snapshot`

职责：

- 返回生命主页所需的完整读取模型
- 成为生命主页、聊天页、设置页的统一状态事实源

至少应稳定提供：

- identity summary
- display name
- emoji
- current state
- attention focus
- active intentions
- relationship summary
- recent changes
- recent memory summaries
- next likely actions
- coherence score
- pacing state

### 5.2 生命写入接口

`POST /api/life/commands`

请求固定为：

- `type`
- `content`
- `context`
- `source`

命令类型固定为：

- `ASK`
- `TASK`
- `RESEARCH`
- `ACTION`
- `LEARNING`
- `DECISION`

响应固定包含：

- `commandResult`
- `impactReport`
- `lifeSnapshot`

### 5.3 自治接口

- `GET /api/life/autonomy/status`
- `POST /api/life/autonomy/pause`
- `POST /api/life/autonomy/resume`
- `GET /api/life/journal`
- `POST /api/life/reset`

### 5.4 模型配置接口

- `GET /api/model/config`
- `PUT /api/model/config`
- `POST /api/model/test`

模型接口继续独立保留，不并入 `/api/life`。  
原因：它既服务生命主链，也承担基础设施配置语义，独立路径更清晰。

## 6. 兼容策略

以下接口继续保留，但全部冻结为兼容层：

- `/api/sprite/*`
- `/api/mobile/*`
- `/api/agent/*`
- `/api/mcp/*`
- `/api/skill/*`

冻结规则：

- 前端主链不得再新增对它们的依赖
- 后端新功能不得默认挂到这些入口上
- 如果必须使用，只能作为 adapter 或过渡桥接，并在文档中显式标记

## 7. Git 与服务器工作流

### 7.1 分支规则

文档基线分支固定为：

- `codex/refactor-masterplan`

后续 worker 分支从该分支切出，命名统一使用：

- `codex/<task-name>`

### 7.2 服务器标准目录

`jd` 上正式工作目录固定为：

- `~/worktrees/sprite-be`
- `~/worktrees/sprite-fe`

历史目录：

- `~/sprite`
- `~/sprite-fe`

只视为旧副本，不再作为正式集成环境。

### 7.3 集成顺序

合并与集成顺序固定为：

1. `be-infra-deploy`
2. `be-life-core`
3. `fe-data-state`
4. `fe-life-shell`

主线程负责：

- 冲突处理
- 联调
- `jd` 部署验收
- 最终合并顺序控制

## 8. 执行纪律

### 8.1 长运行命令纪律

禁止再用“前台等待长驻进程退出”的方式判断服务是否正常启动。

所有长运行验证必须改为：

1. 后台启动或 Compose 启动
2. 端口探针
3. health 探针
4. 日志尾部探针

允许的判断信号包括：

- `localhost:8080` 监听成功
- `/actuator/health` 返回 `UP`
- `/api/life/snapshot` 返回 200
- 最近日志没有继续报错

### 8.2 并行修改纪律

- 不允许多个 worker 修改同一组核心文件
- 主线程只做集成、冲突处理、部署验收和文档维护
- worker 只能在各自 owned paths 内修改
- 兼容层冻结期间不得顺手“顺便重构”

### 8.3 编码与文档纪律

- 新文档统一使用 `UTF-8`
- 新文档内容优先中文，保留必要英文标识
- 文档描述必须面向“数字生命主链”，避免退回系统面板心智

## 9. 当前阻塞与处理原则

### 9.1 已知阻塞

- 本地原仓库 `.git` ACL 导致无法正常开分支
- 本地直连 GitHub 受限，无法直接从 origin 拉取
- 旧代码与新主链并存，且边界尚未彻底收口
- `CommandOrchestrator` 仍未成为真实唯一执行内核
- `Flyway` 尚未正式接回生产链路

### 9.2 当前处理原则

- 服务器侧 Git 基线优先恢复并作为可用集成锚点
- 本地原仓库继续作为事实观察与文档落仓位置
- 本地原仓库的 `.git` ACL 问题，在不破坏工作树的前提下单独处理，不与功能重构混在一起
- 如果本地 Git ACL 仍无法自动修复，暂停所有需要本地原仓库分支操作的步骤，但不阻塞文档、分析和服务器侧集成基线

## 10. 后续执行入口

后续所有实际代码执行以任务包文档为准：

- `SPRITE_V2_AGENT_TASK_PACKS.md`

该文档会定义：

- 每个 worker 的目标
- owned paths
- forbidden paths
- 验收标准
- handoff 模板

本文件只负责给出总设计，不直接承担任务分配职责。
