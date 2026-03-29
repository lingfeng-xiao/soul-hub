# Sprite V2 多 Agent 任务包

版本：`v1.0`  
最后更新：`2026-03-29`  
依赖文档：`SPRITE_V2_REFACTOR_MASTER.md`

## 1. 使用方式

这份文档用于把 Sprite V2 的执行任务切分给多个 worker agent。  
所有 worker 都必须先阅读总设计文档，再只在自己的 owned paths 内修改。

执行顺序固定为：

1. `git-baseline`
2. `refactor-docs`
3. `fe-life-shell`
4. `fe-data-state`
5. `be-life-core`
6. `be-infra-deploy`
7. `integration-smoke`

其中：

- `git-baseline` 是唯一阻塞包
- `refactor-docs` 完成后，其余包才允许正式开工
- `integration-smoke` 只由主线程负责

## 2. 全局规则

### 2.1 分支规则

- 文档基线分支：`codex/refactor-masterplan`
- worker 分支：`codex/<task-name>`

### 2.2 服务器工作目录

- `jd:~/worktrees/sprite-be`
- `jd:~/worktrees/sprite-fe`

### 2.3 统一验收探针

后端：

- `.\mvnw.cmd -o -q test-compile`
- `GET /actuator/health`
- `GET /api/life/snapshot`
- `GET /api/life/autonomy/status`
- `GET /api/model/config`

前端：

- `npm exec tsc -- --noEmit`
- 主链页面可冷启动
- `/`、`/chat`、`/settings` 只依赖生命接口

部署：

- `docker compose up -d`
- MySQL healthy
- 应用 healthy
- 重启后生命状态仍存在

### 2.4 禁止事项

- 禁止在兼容层顺手做大重构
- 禁止多个 worker 改同一组核心文件
- 禁止再用前台阻塞等待式长命令判断服务是否成功启动
- 禁止让主链重新依赖 `/api/sprite`、`/api/agent`、`/api/mcp`、`/api/skill`

## 3. 任务包定义

## 3.1 `git-baseline`

状态：`阻塞包 / 必须先完成`

### 目标

- 恢复可用的 Git 基线
- 固化服务器标准 worktree
- 为后续 worker 分支和推送建立统一入口

### 当前事实

- 本地原始 `sprite-be` 与 `sprite-fe` 存在 `.git` ACL 问题
- 当前无法在原仓库直接 `checkout -b codex/...`
- `jd` 服务器新的 worktree 目录已经建立并切到 `codex/refactor-masterplan`

### Owned Paths / Targets

- 本地仓库 `.git` 元数据排障
- `jd:~/worktrees/sprite-be`
- `jd:~/worktrees/sprite-fe`

### Forbidden

- 不修改业务代码
- 不改应用运行配置

### 实施清单

- 确认服务器 worktree 的 `git status` 正常
- 确认服务器分支为 `codex/refactor-masterplan`
- 记录本地 ACL 问题根因与处理建议
- 如果本地原仓库仍不可修复，则明确“服务器侧 Git 基线为唯一可用基线”

### 验收

- `jd` 两个 worktree 都是正常 Git 仓库
- 可在服务器侧继续切 worker 分支
- 本地问题被明确记录，不再作为隐性阻塞

## 3.2 `refactor-docs`

状态：`主线程负责`

### 目标

- 落地总设计文档
- 落地任务包文档
- 为后续 worker 提供唯一事实基线

### Owned Paths

- `docs/refactor/SPRITE_V2_REFACTOR_MASTER.md`
- `docs/refactor/SPRITE_V2_AGENT_TASK_PACKS.md`

### Forbidden

- 不修改业务代码
- 不改接口实现

### 实施清单

- 固化当前状态快照
- 固化目标架构
- 固化接口契约
- 固化数据与部署决策
- 固化 worker 边界与 handoff 模板

### 验收

- 文档为 `UTF-8`
- 文档内容足够让新 worker 直接开工
- 文档中没有需要实现者自行再做产品决策的空白项

## 3.3 `fe-life-shell`

状态：`可并行`

### 目标

- 收口前端 4 入口产品壳
- 让生命主链页面表达回到“数字生命”而非“系统面板”
- 隔离旧页面入口与旧导航心智

### Owned Paths

- `src/App.tsx`
- `src/components/layout/*`
- `src/pages/LifePage.tsx`
- `src/pages/ChatPage.tsx`
- `src/pages/SettingsPage.tsx`

### Forbidden Paths

- `src/api/*`
- `src/types/api.ts`
- 后端仓库

### 依赖

- 依赖 `refactor-docs`
- 不依赖 `be-infra-deploy`

### 实施清单

- 主导航只保留 4 个入口
- 清除主布局对旧 dashboard / system 面板的残留表达
- 统一页面标题、副标题、状态文案
- 清理主链页面中的旧心智文案
- 确保旧页面只能通过兼容 redirect 到主链，不再作为主导航存在

### 验收

- 主导航只剩 `/`、`/chat`、`/memory`、`/settings`
- 主链页面之间不再暴露旧 dashboard / mcp / skills 入口
- 路由重定向稳定
- `npm exec tsc -- --noEmit` 通过

### 主要风险

- 误触 `src/api/*` 或 `src/types/api.ts`，与 `fe-data-state` 冲突
- 顺手删除旧页面文件，导致兼容 redirect 断裂

## 3.4 `fe-data-state`

状态：`可并行`

### 目标

- 让 React Query 成为前端主链唯一服务端状态来源
- 把旧 API / hooks / stores 从主链依赖图中剥离
- 为 `/`、`/chat`、`/settings` 提供稳定的数据契约

### Owned Paths

- `src/api/spriteApi.ts`
- `src/types/api.ts`
- `src/hooks/useSpriteData.ts`
- `src/lib/constants.ts`
- `src/stores/*`

### Forbidden Paths

- `src/pages/LifePage.tsx`
- `src/pages/ChatPage.tsx`
- 后端仓库

### 依赖

- 依赖 `refactor-docs`

### 实施清单

- 收口 `useLifeSnapshot`
- 收口 `useLifeJournal`
- 收口 `useAutonomyStatus`
- 收口模型配置查询 key 与 invalidation
- 把旧 dashboard/team/agent/mcp API 从主链出口剥离
- 让 `runtimeStore`、`spriteStore` 等退出主链

### 验收

- 主链页面不再依赖 `/api/sprite/state`、`/api/agent/*`
- `useLifeSnapshot`、`useLifeJournal`、`useAutonomyStatus`、模型配置查询构成完整主链
- query key 与 mutation invalidation 一致
- `npm exec tsc -- --noEmit` 通过

### 主要风险

- 改动 `spriteApi.ts` 时误伤旧兼容接口
- 过度删除 store，导致 `/memory` 或测试层直接崩掉

## 3.5 `be-life-core`

状态：`可并行`

### 目标

- 做实生命主链接口与生命快照
- 做实命令执行、日志记录、自治状态、模型配置
- 明确旧 controller 只是兼容层

### Owned Paths

- `src/main/java/com/lingfeng/sprite/controller/LifeController.java`
- `src/main/java/com/lingfeng/sprite/controller/ModelController.java`
- `src/main/java/com/lingfeng/sprite/controller/dto/*`
- `src/main/java/com/lingfeng/sprite/domain/**`
- `src/main/java/com/lingfeng/sprite/life/**`

### Forbidden Paths

- `pom.xml`
- `src/main/resources/**`
- `Dockerfile`
- `docker-compose.yml`

### 依赖

- 依赖 `refactor-docs`
- 与 `be-infra-deploy` 并行，但不得改其 owned paths

### 实施清单

- 让 `GET /api/life/snapshot` 成为稳定读模型
- 让 `POST /api/life/commands` 输出真实 `impactReport`
- 让 `LifeJournalService` 成为可靠事件流
- 清掉主链中的 placeholder / 静态拼装逻辑
- 明确旧 controller 只是 facade / compatibility

### 验收

- `GET /api/life/snapshot` 稳定返回
- `POST /api/life/commands` 稳定返回
- 自治接口稳定返回
- 模型接口稳定返回
- 空状态与已有状态两种情况下快照都可生成
- `.\mvnw.cmd -o -q test-compile` 通过

### 主要风险

- 与旧 `SpriteService` / `ConversationService` / `service.ActionExecutor` 的调用边界不清
- `CommandOrchestrator` 仍是 placeholder，导致“接口可用但能力是假”

## 3.6 `be-infra-deploy`

状态：`可并行`

### 目标

- 切清 `local-h2` 与 `prod-mysql`
- 恢复 Flyway 作为正式 schema authority
- 固化 `jd` 的 compose 与部署目录约定

### Owned Paths

- `pom.xml`
- `src/main/resources/**`
- `Dockerfile`
- `docker-compose.yml`

### Forbidden Paths

- `src/main/java/com/lingfeng/sprite/domain/**`
- 前端仓库

### 依赖

- 依赖 `refactor-docs`

### 实施清单

- 把环境分为 `local-h2` 与 `prod-mysql`
- 让生产环境不再依赖 `ddl-auto:update`
- 恢复 Flyway 并确定唯一迁移目录
- 让 Compose、应用配置、MySQL 目录约定一致
- 把 `jd` 的正式启动方式写清

### 验收

- 本地 profile 仍可快速启动 smoke
- `jd` 上 `docker compose up -d` 后应用连接 MySQL 正常
- 重启后生命状态与 journal 仍保留
- 文档中明确记录 H2 与 MySQL 的职责边界

### 主要风险

- `schema.sql` 与 migration SQL 双权威
- Java 17 / 21 运行口径继续漂移
- Compose 路径与实际服务器目录不一致

## 3.7 `integration-smoke`

状态：`主线程负责 / 最后执行`

### 目标

- 合并 worker 产出
- 处理冲突
- 在本地与 `jd` 完成最终验收

### Owned Scope

- 不固定文件路径
- 负责集成冲突处理与最终验收

### 依赖

- `be-infra-deploy`
- `be-life-core`
- `fe-data-state`
- `fe-life-shell`

### 实施清单

- 按顺序合并任务包
- 修复接口/类型/路由冲突
- 本地做 health / life / model 探针
- `jd` 做 compose 部署验收
- 确认主链页面只依赖生命接口

### 验收

- 本地前后端主链都可打开
- 服务器 compose、MySQL、应用健康检查通过
- 重启后生命状态持久化正常
- 主链页面不再反向依赖旧系统面板

## 4. 推荐并行组合

如果只开 2 个 worker：

- worker-1：`fe-life-shell`
- worker-2：`be-life-core`

如果开 3 个 worker：

- worker-1：`fe-life-shell`
- worker-2：`fe-data-state`
- worker-3：`be-life-core`

如果开 4 个 worker：

- worker-1：`fe-life-shell`
- worker-2：`fe-data-state`
- worker-3：`be-life-core`
- worker-4：`be-infra-deploy`

推荐的正式并行组合是 4 worker。  
因为这四个包的写路径已经足够分离，适合真正并发推进。

## 5. Handoff 模板

每个 worker 完成后必须按以下格式交接：

### 5.1 交接内容

- 目标是否完成
- 改动文件列表
- 核心行为变化
- 运行过的验证命令
- 未解决风险
- 是否需要主线程集成时额外处理

### 5.2 标准模板

```md
任务包：

结果：

改动文件：

行为变化：

验证：

未解决风险：

需要主线程处理的点：
```

## 6. 当前启动建议

当前建议的实际启动顺序如下：

1. 主线程维护 `refactor-docs`
2. 启动 1 个前端壳 worker
3. 启动 1 个前端数据 worker
4. 启动 1 个后端生命主链 worker
5. 条件允许时再启动 1 个基础设施 worker

如果本地原仓库 `.git` ACL 仍未修复，则：

- 继续使用服务器侧 `codex/refactor-masterplan` 作为可用 Git 基线
- 主线程保留本地文档维护与事实验证职责
- 不在原仓库上做需要分支/推送前提的危险操作
