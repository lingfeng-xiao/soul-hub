# Agent Eval 设计模板

> 用于创建高质量的 AI Agent 评估

---

## 1. 基础结构定义

```yaml
Eval Name: [名称]
Purpose: [评估目标]
Agent Type: [coding/research/computer-use/conversational]
```

---

## 2. 核心组件

### Task (任务)
- **定义**: 单个测试，包含定义的输入和成功标准
- **数量建议**: 50-500 个任务
- **覆盖**: 核心功能 + 边界情况 + 已知失败模式

### Trial (尝试)
- **定义**: 每次尝试一个任务
- **样本数**: 建议 5-10 次/任务（减少方差）

### Grader (评分器)
- **类型**:
  - ✅ 单元测试 (代码类)
  - ✅ LLM 评判 (开放类)
  - ✅ 规则匹配 (结构化输出)
  - ✅ 环境验证 (数据库状态等)

### Transcript (轨迹)
- **记录**: 完整交互记录
- **包含**: 输出、工具调用、推理、中间结果

---

## 3. 资源配置清单

| 资源类型 | 建议配置 | 说明 |
|---------|---------|------|
| CPU | 2-4x 基线 | 编译/测试需要 |
| 内存 | 3x 基线 | 避免 OOM |
| 时间限制 | 5-10 分钟/任务 | 复杂任务需要更多 |
| 并发数 | 10-50 | 根据 API 限制 |

### ⚠️ 关键：双重资源配置

```yaml
# ❌ 错误：单一值
resources:
  memory: "4Gi"

# ✅ 正确：分离 Guaranteed 和 Limit
resources:
  memory_guaranteed: "4Gi"
  memory_limit: "12Gi"  # 3x 余量
```

---

## 4. 评估类型检查清单

### Coding Agent
- [ ] 单元测试通过率
- [ ] 文件修改正确性
- [ ] 依赖安装成功
- [ ] 边界情况处理
- [ ] 代码质量 (可选)

### Research Agent
- [ ] 信息检索准确性
- [ ] 总结质量
- [ ] 引用完整性
- [ ] 幻觉率

### Computer Use Agent
- [ ] 任务完成率
- [ ] 步骤效率
- [ ] 错误恢复能力
- [ ] UI 交互准确性

### Conversational Agent
- [ ] 意图识别准确率
- [ ] 响应相关性
- [ ] 上下文保持
- [ ] 用户满意度 (LLM 评判)

---

## 5. 评分器设计

### 单元测试评分器
```python
def grade(task, transcript, environment):
    result = run_tests(task.test_file)
    return {
        "passed": result.all_passed,
        "score": result.pass_rate,
        "details": result.failures
    }
```

### LLM 评判评分器
```python
def grade(task, transcript):
    judgment = llm.judge(
        task=task.description,
        expected=task.criteria,
        actual=transcript.final_output,
        rubric=task.rubric
    )
    return judgment.to_score()
```

---

## 6. 方差控制

### 必须记录
- [ ] 硬件规格 (CPU/RAM/GPU)
- [ ] 资源配置 (guaranteed/limit)
- [ ] 模型版本和温度
- [ ] 运行时间/日期
- [ ] 并发级别

### 建议实践
- [ ] 在多个时间点运行
- [ ] 报告置信区间
- [ ] 设置对照实验

---

## 7. 质量指标

| 指标 | 目标 |
|-----|------|
| 基础设施错误率 | < 2% |
| 评分者间一致性 | > 80% |
| 重测稳定性 | ±2% |

---

## 8. 快速开始模板

```yaml
# eval-config.yaml
name: "My Agent Eval"
version: "1.0"

agent:
  type: "coding"
  harness: "claude-code"
  
tasks:
  source: "./tasks/*.json"
  count: 100
  
resources:
  cpu: "2"
  memory_guaranteed: "4Gi"
  memory_limit: "12Gi"
  timeout: 300
  
grading:
  primary:
    type: "unit_test"
    weight: 0.8
  secondary:
    type: "llm_judge"
    weight: 0.2
    
run:
  trials_per_task: 5
  concurrency: 20
```

---

## 参考资料

- Anthropic: [Demystifying Evals for AI Agents](https://www.anthropic.com/engineering/demystifying-evals-for-ai-agents)
- Anthropic: [Infrastructure Noise in Agentic Evals](https://www.anthropic.com/engineering/infrastructure-noise)
- SWE-bench: 编码评估基准
- Terminal-Bench: 终端任务评估

---

*2026-03-18 | 模板编号: eval-001*
