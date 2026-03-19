# 多 Agent 并行协作系统实战：从零构建 C 编译器

> 来源: Anthropic Engineering - Building a C compiler with a team of parallel Claudes (2026-02-05)

## 核心成果

- **项目**: 使用 16 个并行 Claude Agent 从零构建 Rust C 编译器
- **规模**: 100,000 行代码
- **成就**: 可编译 Linux 6.9 (x86, ARM, RISC-V)
- **成本**: ~$20,000 API 费用，~2000 次 Claude Code 会话

## 架构设计

### 1. 自主运行循环 (Autonomous Loop)

```bash
while true; do
  COMMIT=$(git rev-parse --short=6 HEAD)
  LOGFILE="agent_logs/agent_${COMMIT}.log"
  claude --dangerously-skip-permissions \
    -p "$(cat AGENT_PROMPT.md)" \
    --model claude-opus-X-Y &> "$LOGFILE"
done
```

**关键点**:
- 任务完成后自动获取下一个，无需人工介入
- Agent 自主将大问题拆解为小任务
- 持续运行直到任务完美完成

### 2. 任务锁机制 (Git-based Locking)

```python
# 获取任务锁
def acquire_lock(task_name):
    lock_file = f"current_tasks/{task_name}.txt"
    # 写入当前 agent 标识
    # 如果 git push 失败，说明被其他 agent 抢占

# 释放锁
def release_lock(task_name):
    # git pull 合并其他 agent 的更改
    # git push 推送自己的更改
    # 删除锁文件
```

**核心逻辑**:
1. Agent 通过写入 `current_tasks/` 目录"锁定"任务
2. 多个 Agent 同时尝试时，git 同步机制自动冲突检测
3. 冲突后 Agent 自动选择其他任务
4. Merge conflict 由 Agent 智能处理

### 3. 容器化隔离

- 每个 Agent 在独立 Docker 容器中运行
- 共享代码仓库通过 volume mount
- 避免环境污染和状态泄露

## 三大核心教训

### Lesson 1: 编写极高测试质量

> "Claude 会自主解决任何你给的问题，所以任务验证器必须接近完美"

**实践**:
- 使用成熟的测试套件 (GCC, LLVM test suite)
- 构建 CI 管道持续集成
- 发现 Claude 犯错模式 → 立即编写针对性测试
- 新提交不能破坏已有功能

**关键指标**: 99% 测试通过率后才进入下一阶段

### Lesson 2: 站在 Claude 角度设计

| 人类习惯 | Claude 视角问题 | 解决方案 |
|---------|----------------|---------|
| 看全部日志 | 上下文污染 | 最多输出几行，详细信息存文件 |
| 看时间 | 时间盲区 | 添加 --fast 选项 (1-10% 采样) |
| 看全部代码 | 定位困难 | 预计算聚合统计信息 |
| 从头了解项目 | 初始化慢 | 维护详细的 README 和进度文件 |

**上下文污染**:
- 打印数千行无用输出会淹没关键信息
- 日志格式: `ERROR: <reason>` 便于 grep
- 关键信息写入文件，Agent 需要时读取

### Lesson 3: 简化并行化

**阶段 1 - 独立测试** (简单并行):
- 每个 Agent 分配不同失败测试
- 天然无冲突

**阶段 2 - 复杂任务** (需要策略):
- 问题: 16 Agent 同时卡在同一 bug
- 解决: 使用 "oracle" 比对 (GCC 作为参考编译器)
- 随机编译大部分 kernel 文件，用 Claude 编译剩余文件
- 如果失败，逐步缩小问题范围

## 可复用的模式

### 模式 1: 任务队列 + 锁

```
task_queue/          # 待完成任务
current_tasks/       # 当前进行中 (锁)
completed/          # 已完成
failed/             # 失败重试
```

### 模式 2: Oracle 比对

```
baseline_output = oracle(input)
agent_output = agent(input)
diff = compare(baseline_output, agent_output)
```

### 模式 3: 分层测试

```
Level 1: 单元测试 (快速反馈)
Level 2: 集成测试 (模块协作)
Level 3: 系统测试 (完整功能)
Level 4: 回归测试 (保护已有功能)
```

## 适用场景

| 场景 | 适用性 | 说明 |
|------|--------|------|
| 大型代码库重构 | ✅ 极佳 | 多 Agent 并行处理不同模块 |
| 复杂算法实现 | ✅ 适用 | 需要多种测试用例验证 |
| 文档/代码维护 | ✅ 适用 | 可分离关注点 |
| 简单脚本任务 | ❌ 过度 | 单 Agent 足够 |

## 延伸阅读

- [Anthropic 多 Agent 研究系统](https://www.anthropic.com/engineering/multi-agent-research-system)
- [长时运行 Agent 最佳实践](https://www.anthropic.com/engineering/effective-harnesses-for-long-running-agents)
- [Claude Code 最佳实践](https://www.anthropic.com/engineering/claude-code-best-practices)

---

*本文档为知识沉淀，非直接执行指令*
