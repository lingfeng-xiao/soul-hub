# 基础设施噪声：资源配置对 Agentic 评估的影响

## 核心要点
- **资源配置可改变评估分数高达 6 个百分点**，有时甚至超过 leaderboard 差距
- 容器运行时使用两个参数：guaranteed allocation（预留资源）和 hard limit（杀掉阈值）
- 当两者设为相同值时，零头寸导致瞬时内存波动即可 OOM kill

## 关键数据
| 配置 | 基础设施错误率 | 成功率提升 |
|------|--------------|-----------|
| 1x (严格) | 5.8% | 基准 |
| 3x 头寸 | 2.1% | 仍在噪声范围内 |
| 无限制 | 0.5% | +6个百分点 |

## 应用建议（对管管）
1. **分离资源参数**：guaranteed allocation ≠ hard limit
2. **3x 头寸原则**：基础设施错误降 2/3，分数变化在噪声内
3. **小差异需谨慎**：<3分的 leaderboard 差异可能只是硬件更强

## 模板：Cron 任务资源配置检查清单
```yaml
# 检查任务资源配置
- 是否指定了 guaranteed allocation？
- hard limit 与 guaranteed 的比例是多少？
- 是否需要调整以减少基础设施噪声？
```

---
来源: Anthropic Engineering Blog (2026-03)
