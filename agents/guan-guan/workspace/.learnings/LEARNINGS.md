# LEARNINGS.md

<!-- SCHEMA: {"type":"learning"} -->

---

## 📋 使用方法

このファイルは `proactive-self-improving-agent` skill によって自動的に更新されます。

---

## 本轮教训 (2026-03-18)

### 问题 1：web_search API 不可用

**现象：** Brave Search API key 未配置，搜索功能全部失败

**解决：** 
- 启用备用方案：整理现有知识库
- 更新 evolution-plan.md 添加备用流程

**教训：** 
- 依赖外部 API 需要考虑失败情况
- 进化系统应该有降级策略

---

### 问题 2：web_fetch 成功替代搜索

**现象：** web_fetch 可以直接抓取 Anthropic 工程博客，内容质量极高

**解决：**
- 直接抓取 anthropic.com/engineering 获取最新 AI Agent 工程实践
- 产出 1 篇深度笔记 + 1 个模板

**经验：**
- Anthropic 工程博客是高质量信息来源，优先抓取
- 每轮只需抓取 1-2 个主题，深入产出比广泛撒网更有价值

**推荐来源：**
- https://www.anthropic.com/engineering （工程博客首页）
- https://www.anthropic.com/engineering/building-effective-agents
- https://www.anthropic.com/engineering/infrastructure-noise （本轮产出）
- https://www.anthropic.com/engineering/demystifying-evals-for-ai-agents

---

### 时间效率优化

**发现：** 本轮 20 分钟时间分配
- 搜索/抓取: 3 min
- 阅读提取: 3 min  
- 写作产出: 8 min
- 反思优化: 2 min
- 缓冲: 4 min

**优化建议：** 写作环节可以产出 1 篇笔记 + 1 个模板，时间分配合理
