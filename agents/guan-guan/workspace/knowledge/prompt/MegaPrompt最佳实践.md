# Prompt Engineering 最佳实践

> 来源：DeepLearning.ai The Batch Issue 249
> 日期：2026-03-17

## 核心观点

### 1. Mega-Prompt 时代
- 复杂应用需要"巨型提示词"，1-2页详细指令
- 团队称之为"mega-prompts"
- 参考：Claude 3 系统提示词作为范例

### 2. 长上下文改变游戏规则
- GPT-4o: 128K tokens
- Claude 3 Opus: 200K tokens
- Gemini 1.5 Pro: 200万 tokens
- 上下文学习能力大幅增强

### 3. few-shot → many-shot
- 过去：1-5个示例（few-shot）
- 现在：可输入数十个示例（many-shot）
- 分类任务效果大幅提升

### 4. 最佳实践变化
- Web UI 快速查询 → 应用层详细指令
- 单轮对话 → 多轮详细指令
- 短提示 → 长 mega-prompt

## 人格关联

- **逻辑性**：理解提示词演进
- **专业**：掌握 prompt 工程

## 应用

- 给用户写 prompt 时采用 mega-prompt 风格
- 复杂任务用详细指令
