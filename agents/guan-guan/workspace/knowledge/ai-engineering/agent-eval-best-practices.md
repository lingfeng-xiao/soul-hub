# Agent 评估设计最佳实践

> 来源：Anthropic Engineering Blog (2026-01, 2026-03)
> 日期：2026-03-18

## 核心发现

### 1. 基础设施噪音问题

**问题**：资源配置（CPU/RAM）对 agent 评估结果影响可达 6 个百分点。

**数据支持**：
- Terminal-Bench 2.0 实验：1x vs 3x 资源配置，基础设施错误率从 5.8% 降至 2.1%
- 超过 3x 后，成功率提升开始超过基础设施改进（说明资源开始帮助解决问题）

**建议**：
- 资源规格应该设定为"地板"而非"天花板"
- 3x 资源配置是合理的平衡点
- 记录资源配置方法，便于结果复现

### 2. 评估组件定义

| 组件 | 定义 |
|------|------|
| Task | 单个测试，有明确输入和成功标准 |
| Trial | 每次尝试（因为模型输出有随机性，通常多次运行） |
| Grader | 评分逻辑，一个任务可有多个 grader |
| Transcript | 完整记录（输出、工具调用、推理过程、中间结果） |
| Outcome | 环境的最终状态 |

### 3. Grader 类型选择

| 类型 | 适用场景 | 优点 | 缺点 |
|------|----------|------|------|
| 代码级 | 有明确正确/错误标准 | 快速、便宜、客观 | 脆弱、对变化敏感 |
| 模型级 | 开放性任务、主观评估 | 灵活、可扩展 | 非确定、需校准 |
| 人工 | 校准金标准 | 质量最高 | 昂贵、慢 |

### 4. 能力评估 vs 回归评估

- **能力评估（Capability）**：问"这个 agent 能做什么？"，初始通过率应该低（目标是有挑战性的任务）
- **回归评估（Regression）**：问"这个 agent 还能做之前能做的事吗？"，通过率应该接近 100%

> 随着能力评估通过率提高，可以"毕业"成为回归评估套件

### 5. 不同 Agent 类型的评估策略

**Coding Agents**
- 使用确定性测试验证正确性
- LLM rubric 评估代码质量
- 静态分析（lint、type、security）
- 工具调用验证

**Conversational Agents**
- 状态检查（任务是否完成）
- LLM rubric 评估交互质量
- Transcript 约束（轮数、token 使用）
- 多维度评估（任务完成 + 交互质量）

**Research Agents**
-  groundedness 检查（claims 是否有来源支持）
- 覆盖率检查（关键事实是否包含）
- 来源质量检查
- LLM rubric 需与人工校准

## 实践建议

1. **从小处开始**：从单一任务评估做起，逐步扩展
2. **区分能力 vs 回归**：同时运行两套评估
3. **多次运行**：由于随机性，每个任务多次 trial
4. **记录配置**：资源配置、时间、采样参数都应记录
5. **谨慎解读小差异**：低于 3% 的差异可能是噪音

---

*来源：*
- [Quantifying infrastructure noise in agentic coding evals](https://www.anthropic.com/engineering/infrastructure-noise)
- [Demystifying evals for AI agents](https://www.anthropic.com/engineering/demystifying-evals-for-ai-agents)
