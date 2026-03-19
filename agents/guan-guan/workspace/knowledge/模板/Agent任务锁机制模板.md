# Agent 任务锁机制模板

> 基于 Anthropic 多 Agent C 编译器项目实战提取

## 使用场景

- 多 Agent 并行处理同一代码库
- 任务需要互斥，避免重复劳动
- 无中心协调器情况下的分布式协作

## 核心原理

```
Git 冲突检测 = 分布式锁
文件存在性 = 锁状态
原子操作 = git add + commit + push
```

## 实现模板

### Python 版本

```python
import os
import subprocess
import time
from pathlib import Path

class AgentTaskLock:
    def __init__(self, repo_path, task_dir="current_tasks"):
        self.repo_path = Path(repo_path)
        self.task_dir = self.repo_path / task_dir
        self.task_dir.mkdir(exist_ok=True)
    
    def acquire_lock(self, task_name, agent_id):
        """尝试获取任务锁"""
        lock_file = self.task_dir / f"{task_name}.lock"
        
        # 尝试写入锁文件
        try:
            lock_file.write_text(f"{agent_id}\n{time.time()}")
            # 尝试推送到远程
            subprocess.run(
                ["git", "add", str(lock_file)],
                cwd=self.repo_path, check=True
            )
            result = subprocess.run(
                ["git", "commit", "-m", f"Agent {agent_id} locks {task_name}"],
                cwd=self.repo_path, capture_output=True
            )
            result = subprocess.run(
                ["git", "push"],
                cwd=self.repo_path, capture_output=True
            )
            if result.returncode == 0:
                return True
        except:
            pass
        
        # 失败，说明被抢占
        self._cleanup_lock(lock_file)
        return False
    
    def release_lock(self, task_name, agent_id):
        """释放任务锁"""
        lock_file = self.task_dir / f"{task_name}.lock"
        if lock_file.exists():
            lock_file.unlink()
        subprocess.run(["git", "add", "-A"], cwd=self.repo_path, check=True)
        subprocess.run(
            ["git", "commit", "-m", f"Agent {agent_id} releases {task_name}"],
            cwd=self.repo_path, capture_output=True
        )
        subprocess.run(["git", "push"], cwd=self.repo_path, capture_output=True)
    
    def get_available_tasks(self):
        """获取所有未锁定的任务"""
        all_tasks = set(f.stem for f in self.task_dir.glob("*.lock"))
        return list(set(self._list_all_tasks()) - all_tasks)
    
    def _list_all_tasks(self):
        """列出所有可能的任务"""
        # 实际实现根据项目调整
        return []
    
    def _cleanup_lock(self, lock_file):
        """清理无效锁"""
        try:
            if lock_file.exists():
                lock_file.unlink()
        except:
            pass
```

### Shell 脚本版本 (Anthropic 原始版)

```bash
#!/bin/bash
# agent_loop.sh - Claude 自主运行循环

while true; do
  # 获取当前 commit hash
  COMMIT=$(git rev-parse --short=6 HEAD)
  LOGFILE="agent_logs/agent_${COMMIT}.log"
  
  # 读取任务提示
  claude --dangerously-skip-permissions \
    -p "$(cat AGENT_PROMPT.md)" \
    --model claude-opus-X-Y &> "$LOGFILE"
  
  # 任务完成后短暂休息
  sleep 1
done
```

## 任务状态机

```
┌─────────────┐     acquire      ┌─────────────┐
│   AVAILABLE │ ───────────────→ │   LOCKED    │
└─────────────┘                  └─────────────┘
                                      │
                              work on task
                                      │
                                      ▼
                               ┌─────────────┐
                               │   WORKING   │
                               └─────────────┘
                                      │
                              release lock
                                      ▼
                               ┌─────────────┐
                               │  COMPLETED  │
                               └─────────────┘
```

## 冲突处理策略

### 1. 立即放弃 (Fast Fail)

```python
if not acquire_lock(task):
    # 立即选择其他任务
    task = choose_other_task()
```

### 2. 等待重试 (Retry with Backoff)

```python
for attempt in range(3):
    if acquire_lock(task):
        return True
    sleep(backoff(attempt))
return False
```

### 3. 智能重试 (Smart Retry)

```python
# 分析当前任务分布
tasks = get_locked_tasks()
# 找人数最少的任务类型
target = min(tasks, key=lambda t: t.lock_count)
```

## 高级模式

### Oracle 模式 (Anthropic 实际使用)

```python
def test_with_oracle(agent_compiler, oracle_compiler, target_files):
    """使用参考编译器比对结果"""
    # 随机选择部分文件用 Agent 编译
    agent_files = random.sample(target_files, k=len(target_files)//10)
    baseline_files = set(target_files) - set(agent_files)
    
    # 用 Oracle 编译 baseline
    oracle_compile(baseline_files)
    
    # 用 Agent 编译小部分
    agent_result = agent_compile(agent_files)
    
    if not agent_result.success:
        # 逐步精确定位问题
        refine_and_retry(agent_files)
```

### 分层锁模式

```
global_tasks/     # 顶层任务 (里程碑)
  └─ milestone_1/
      local_tasks/  # 子任务 (具体实现)
```

## 最佳实践

1. **锁粒度适中**: 任务太大 → 冲突多；任务太小 → 开销大
2. **超时机制**: 长时间未完成 → 自动释放
3. **日志记录**: 所有操作写入日志，便于调试
4. **优雅降级**: 锁机制失败时仍能工作

---

*模板持续更新中...*
