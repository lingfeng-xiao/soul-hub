# 95-内存趋势感知实现笔记

> 2026-03-19 学习产出

## 背景

从多次进化中发现：内存持续在85%-95%之间波动，但感知系统只检测**当前值**，没有检测**趋势**。

## 问题

- 感知显示：内存88%
- 用户问：内存是不是在上升？
- 系统无法回答

## 解决

### 1. 添加趋势检测函数

```python
def get_memory_trend():
    """分析最近N次感知记录，判断内存走势"""
    memory = load_memory()
    recent = memory.get("perceptions", [])[-10:]
    
    # 提取内存值
    mem_values = [p['hardware']['memory']['used_pct'] for p in recent]
    
    # 比较前半和后半均值
    mid = len(mem_values) // 2
    first_half = sum(mem_values[:mid]) / mid
    second_half = sum(mem_values[mid:]) / (len(mem_values) - mid)
    
    diff = second_half - first_half
    
    if diff > 2: return "rising", diff
    elif diff < -2: return "falling", abs(diff)
    else: return "stable", diff
```

### 2. 在感受中显示趋势

```python
# 原来
if mem_pct > 80:
    feelings.append("😣 内存有点撑")

# 改进
if mem_pct > 80:
    if mem_trend == "rising":
        feelings.append(f"😰 内存持续上升({mem_trend_val}%)")
    elif mem_trend == "falling":
        feelings.append(f"😊 内存正在下降({mem_trend_val}%)")
    else:
        feelings.append("😣 内存有点撑")
```

## 结果

现在感知显示：`😰 内存持续上升(2.2%)`

## 核心领悟

> 感知不仅是检测当前状态，还要能预判趋势
> 从"静态感知"升级为"动态感知"
