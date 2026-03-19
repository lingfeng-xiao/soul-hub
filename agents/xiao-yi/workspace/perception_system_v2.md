# 电脑精灵全维度感知系统 - 优化方案 v2

## 优化背景

基于对业界监控系统（Windows任务管理器、macOS活动监视器、Cloudflare Magic WAN监控、Prometheus+Grafana等）的分析，优化了原方案。

---

## 核心改进

### 1. 架构优化：采用"推拉结合"

| 原方案 | 优化后 | 理由 |
|--------|--------|------|
| 纯定时轮询 | 事件驱动 + 定时上报 | 减少资源浪费，实时响应 |
| 单线程采集 | 多通道并行 | 避免感知延迟 |

### 2. 感知增强：新增关键维度

| 新增维度 | 实现方式 | 价值 |
|----------|----------|------|
| **应用使用时长** | 窗口焦点跟踪 | 了解用户习惯 |
| **电池/电源** | WMI查询 | 笔记本用户刚需 |
| **温度/风扇** | WMI/IOKit | 硬件健康 |
| **启动项** | 注册表/LaunchAgents | 优化开机速度 |
| **日志监控** | Windows Event Log | 异常根因 |

### 3. 存储优化：分级存储

```
热数据 (最近1小时)     → 内存 + SQLite
温数据 (最近7天)        → SQLite + 压缩
冷数据 (超过7天)         → 归档或删除

截图策略：
- 每15分钟1张，保留最近24小时
- 超过24小时压缩为缩略图
- 超过7天删除
```

### 4. 隐私保护：本地优先 + 选择性感知

```
✅ 采集：系统资源、进程名称、窗口标题
⚠️ 谨慎：屏幕截图（可配置关闭）
❌ 不采集：键盘输入内容、密码框内容、隐私应用
```

---

## 优化后的架构

```
┌─────────────────────────────────────────────────────────┐
│                    感知中枢 (Perception Hub)              │
├─────────────────────────────────────────────────────────┤
│  ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐  │
│  │ 视觉感知  │ │ 进程感知  │ │ 网络感知  │ │ 行为感知  │  │
│  │ (截图)   │ │ (进程)   │ │ (网络)   │ │ (窗口)   │  │
│  └────┬─────┘ └────┬─────┘ └────┬─────┘ └────┬─────┘  │
│       │            │            │            │         │
│       └────────────┴────────────┴────────────┘         │
│                        │                               │
│              ┌─────────▼─────────┐                    │
│              │   事件总线 (Event Bus) │                │
│              └─────────┬─────────┘                    │
│                        │                               │
│       ┌────────────────┼────────────────┐              │
│       │                │                │              │
│  ┌────▼────┐    ┌─────▼─────┐   ┌─────▼─────┐       │
│  │ 实时告警 │    │ 数据存储   │   │ AI理解引擎 │       │
│  │(即时通知)│    │ (SQLite)  │   │ (图像+语义)│       │
│  └─────────┘    └───────────┘   └───────────┘       │
└─────────────────────────────────────────────────────────┘
```

---

## 关键模块优化

### 模块1：轻量级感知器 (PerceptionHub)

```python
class PerceptionHub:
    """采用观察者模式，事件驱动 + 定时上报"""
    
    def __init__(self):
        self.sensors = {
            'screenshot': ScreenshotSensor(interval=900),
            'process': ProcessSensor(interval=900),
            'window': WindowSensor(interval=60),
            'resource': ResourceSensor(interval=300),
            'network': NetworkSensor(interval=900),
            'power': PowerSensor(interval=300),
        }
        self.event_bus = EventBus()  # 事件总线
    
    def start(self):
        # 并行启动所有传感器
        for sensor in self.sensors.values():
            sensor.start(callback=self.event_bus.publish)
```

### 模块2：AI理解引擎 (AIUnderstandingEngine)

```python
class AIUnderstandingEngine:
    """分层理解：图像 → 语义 → 关联 → 建议"""
    
    def understand_screenshot(self, image_path):
        # 1. 图像理解：桌面上有什么？
        visual_info = self.vision_model.analyze(image_path)
        
        # 2. 语义理解：这代表什么状态？
        context = self.translate_to_context(visual_info)
        
        # 3. 关联分析：和之前比有什么变化？
        changes = self.detect_changes(context)
        
        # 4. 生成建议
        if changes.has_issues:
            return self.generate_suggestions(changes)
        
        return context
```

### 模块3：智能告警 (SmartAlerter)

```python
class SmartAlerter:
    """基于基线的异常检测，减少误报"""
    
    def __init__(self):
        self.baseline = BaselineManager()  # 学习正常状态
        self.rules = AlertRules()
    
    def check(self, metric):
        # 学习阶段：收集数据建立基线
        if not self.baseline.is_ready():
            self.baseline.add(metric)
            return None  # 不告警
        
        # 检测阶段：对比基线
        if self.baseline.is_anomaly(metric):
            return self.rules.get_alert(metric)
        
        return None
```

---

## 实施计划（优化版）

### Week 1: 基础设施
- [x] 事件总线框架
- [x] 核心传感器（进程/窗口/资源）
- [x] SQLite存储层

### Week 2: 感知增强
- [ ] 截图传感器
- [ ] 网络传感器
- [ ] 电源传感器

### Week 3: AI理解
- [ ] 图像理解集成
- [ ] 语义翻译层
- [ ] 变化检测

### Week 4: 智能告警
- [ ] 基线学习
- [ ] 告警规则引擎
- [ ] 通知输出

---

## 与原方案对比

| 维度 | v1 (原) | v2 (优化) |
|------|---------|-----------|
| 架构 | 单线程轮询 | 事件驱动 + 并行 |
| 存储 | 单一存储 | 分级存储（热/温/冷） |
| 隐私 | 未考虑 | 选择性感知 |
| 异常 | 阈值告警 | 基线学习 + 智能告警 |
| 可扩展 | 差 | 插件化传感器 |

---

## 技术选型确认

| 组件 | 选型 | 理由 |
|------|------|------|
| 事件总线 | 自研(轻量) | 简单够用 |
| 存储 | SQLite | 轻量、跨平台 |
| 图像理解 | OpenClaw Image | 已有集成 |
| 监控库 | psutil | Python跨平台事实标准 |
| 定时 | OpenClaw Cron | 集成现有系统 |

---

## 总结

v2 核心改进：
1. **事件驱动架构** — 更实时、更高效
2. **分级存储** — 平衡性能与存储
3. **隐私优先** — 选择性感知，本地处理
4. **智能告警** — 基于学习，减少打扰
5. **可扩展** — 插件化设计，方便新增传感器
