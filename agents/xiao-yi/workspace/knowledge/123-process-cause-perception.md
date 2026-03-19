# 进程级感知强化：从知道"什么"到知道"为什么"

## 核心概念

传统感知告诉你"内存80%"——这是**结果**。
进程级感知告诉你"ChatGPT占用691MB导致内存上升"——这是**原因**。

从结果感知升级为原因感知，是感知系统进化的关键里程碑。

---

## 为什么需要原因感知？

| 维度 | 结果感知 | 原因感知 |
|------|----------|----------|
| 内存高 | ❌ 知道高 | ✅ 知道谁干的 |
| CPU高 | ❌ 知道高 | ✅ 知道哪个进程 |
| 卡顿 | ❌ 知道卡 | ✅ 知道为什么卡 |
| 决策 | 建议"降低内存" | 直接结束特定进程 |

**本质区别**：原因感知让行动更精准，不再依赖用户手动排查。

---

## 实现路径

### 1. 进程数据获取

```python
import psutil

# 获取所有进程
for proc in psutil.process_iter(['pid', 'name', 'memory_info']):
    print(proc.info)
```

### 2. 关键指标

- **内存占用**：process.memory_info().rss
- **CPU使用率**：process.cpu_percent()
- **进程年龄**：process.create_time()
- **父子关系**：process.parent()

### 3. 智能过滤

```python
# 排除系统进程，只关注用户应用
user_apps = ['ChatGPT', 'ClawX', 'WeChat', 'Chrome', 'Edge']

def is_user_app(proc):
    try:
        return any(app in proc.name() for app in user_apps)
    except:
        return False
```

---

## 决策升级

**旧决策**：
```
🟡 内存80% → 建议用户关闭应用
```

**新决策**：
```
🟡 内存80% → ChatGPT(691MB) + ClawX(574MB)占用最高
   → 建议关闭ChatGPT，可释放8%内存
```

---

## 感知层强化

| 感知层 | 能力 |
|--------|------|
| 硬件层 | CPU/内存/磁盘百分比 |
| 应用层 | 进程列表+占用 |
| 原因层 | 变化溯源（谁导致的变化）|
| 预测层 | 趋势预测（即将满）|

---

## 下一步

1. 在perception.py中添加进程级检测
2. 实现变化溯源（对比前后状态）
3. 生成可执行建议（不只是"内存高"）

---

*从感知"什么"到感知"为什么"，是质的飞跃*
