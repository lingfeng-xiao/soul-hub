# 82-PowerShell进程监控强化感知

> 2026-03-19 | 主题：系统优化-进程感知强化

## 核心知识

### Get-Process 进阶用法

```powershell
# 1. 获取高内存进程（>100MB）
Get-Process | Where-Object {$_.WorkingSet64 -gt 100MB} | Sort-Object WorkingSet64 -Descending

# 2. 获取进程所属用户（需要管理员权限）
Get-Process -IncludeUserName | Select-Object Name,@{N="User";E={$_.UserName}}

# 3. 获取进程加载的模块（检测可疑模块）
Get-Process chrome -Module | Select-Object ModuleName,FileVersion

# 4. 按内存排序TOP10
Get-Process | Sort-Object WorkingSet64 -Descending | Select-Object -First 10 Name,Id,@{N="Memory(MB)";E={[math]::Round($_.WS/1MB,2)}}
```

### 关键属性
- `WorkingSet64` / `WS`: 工作集内存（物理内存+虚拟内存）
- `PrivateMemorySize`: 私有内存
- `CPU`: CPU使用时间
- `StartTime`: 进程启动时间
- `Responding`: 进程是否响应

---

## 感知强化方向

### 现有能力
- 已有：检测 >100MB 进程并按内存排序
- 已有：跳过系统关键进程列表

### 强化方向

#### 1. 进程用户识别
**知识→感知转化**：
- 当前代码只按进程名判断是否可关闭
- **强化**：通过 -IncludeUserName 判断进程是否属于当前用户
- 只有当前用户的进程才自动关闭，系统进程不动

```python
# 强化代码示例
result = subprocess.run(
    ['powershell', '-Command', 
     'Get-Process | Where-Object {$_.WorkingSet64 -gt 100MB} | '
     'Sort-Object WorkingSet64 -Descending | '
     'Select-Object -First 10 Name,Id,@{N="Memory(MB)";E={[math]::Round($_.WS/1MB,2)}},Responding | '
     'ConvertTo-Json'],
    ...
)
```

#### 2. 进程响应性检测
- 添加 `Responding` 属性检测
- 对于不响应的进程，优先清理

#### 3. 进程运行时长分析
- 识别长期运行的进程（如24小时+）
- 对长期未使用的应用建议关闭

---

## 感知闭环

| 知识 | 现有能力 | 强化后能力 |
|------|----------|------------|
| WorkingSet64 | 检测高内存进程 | ✅ 已有 |
| -IncludeUserName | 无 | 新增用户识别 |
| Responding | 无 | 新增响应性检测 |
| StartTime | 无 | 新增运行时长分析 |

---

## 行动计划

- [ ] 在 auto_cleanup_memory 中添加用户识别
- [ ] 添加进程响应性检测
- [ ] 记录高频问题进程到记忆

---

*知识不是话术，是感知能力的强化原料*
