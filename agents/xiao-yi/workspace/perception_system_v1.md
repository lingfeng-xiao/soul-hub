# 电脑精灵全维度感知系统 - 初始方案

## 目标
让小艺具备"第六感"，能持续感知电脑状态，发现问题主动汇报。

---

## 架构设计

### 1. 感知层（数据采集）

| 维度 | 采集方式 | 频率 | 存储 |
|------|----------|------|------|
| 屏幕截图 | PowerShell截图 | 每15分钟 | 图片文件 |
| 活跃窗口 | Get-Process + Win32 API | 每分钟 | JSON |
| 进程列表 | Get-Process | 每15分钟 | JSON |
| 系统资源 | Get-Counter (CPU/内存/磁盘) | 每5分钟 | JSON |
| 文件变化 | FileSystemWatcher | 实时 | 日志 |
| 网络状态 | netstat | 每15分钟 | JSON |
| 用户活动 | 鼠标/键盘钩子 | 持续 | 聚合统计 |

### 2. 理解层（AI分析）

- **图像理解**：用CV模型理解截图内容（桌面有啥、界面状态）
- **文本理解**：理解窗口标题、进程名称的含义
- **异常检测**：对比历史基线，发现异常模式

### 3. 行动层（输出）

- **日报**：每天总结电脑状态
- **告警**：异常时即时通知
- **建议**：基于感知结果给出优化建议

---

## 关键模块设计

### 模块1：定时感知器 (PerceptionAgent)

```python
class PerceptionAgent:
    def __init__(self):
        self.intervals = {
            'screenshot': 900,    # 15分钟
            'process': 900,       # 15分钟  
            'window': 60,         # 1分钟
            'resource': 300,      # 5分钟
            'network': 900,       # 15分钟
        }
    
    def run(self):
        while True:
            # 定时执行各感知任务
            pass
```

### 模块2：图像理解器 (VisionAnalyzer)

- 截屏 → 保存到本地
- 调用图像模型分析："桌面上有什么应用？"
- 提取关键信息存入数据库

### 模块3：状态数据库 (StateDB)

```
表结构：
- screenshots: id, path, timestamp, ai_summary
- processes: id, name, cpu, memory, timestamp
- windows: id, title, process_name, timestamp
- resources: id, cpu%, memory%, disk%, timestamp
- anomalies: id, type, description, timestamp
```

### 模块4：异常检测器 (AnomalyDetector)

- 建立"正常基线"（平均CPU/内存/常见进程）
- 新数据对比基线，超过阈值则告警

---

## 实施计划

### Phase 1: 基础感知（1周）
- [ ] 定时截图功能
- [ ] 进程列表采集
- [ ] 活跃窗口记录
- [ ] 简单日报输出

### Phase 2: 智能理解（2周）
- [ ] 图像AI理解截图
- [ ] 窗口标题语义分析
- [ ] 异常模式检测

### Phase 3: 主动行动（3周）
- [ ] 异常实时告警
- [ ] 优化建议生成
- [ ] 记忆沉淀与学习

---

## 技术选型

| 组件 | 选型 | 理由 |
|------|------|------|
| 定时任务 | OpenClaw Cron | 集成现有系统 |
| 数据库 | SQLite | 轻量，无需额外部署 |
| 图像理解 | OpenClaw Image模型 | 已有集成 |
| 存储 | 本地文件 | 隐私优先 |

---

## 挑战与风险

1. **隐私**：持续截图可能涉及敏感信息 → 本地存储，不上传云
2. **性能**：频繁截图影响性能 → 优化截图频率和压缩比
3. **存储**：图片占用空间 → 定期清理，只保留关键帧
4. **误报**：异常检测太敏感 → 逐步建立基线，调优阈值
