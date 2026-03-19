# Contextual Retrieval 上下文检索

> 来源：Anthropic Engineering Blog
> 日期：2026-03-17

## 核心观点

### 1. RAG 问题
- 传统 RAG 编码时丢失上下文
- 导致检索失败

### 2. Contextual Retrieval
- 两种子技术：Contextual Embeddings + Contextual BM25
- 减少 49% 检索失败
- 结合 reranking 减少 67%

### 3. 超过 200K tokens 怎么办
- 小知识库：直接放入 prompt
- Prompt Caching：缓存常用 prompt
- 延迟降低 2x，成本降低 90%

### 4. BM25
- 词汇匹配 vs 语义嵌入
- 精确匹配（术语、ID）
- 解决 embedding 遗漏的精确匹配

### 5. 混合方案
- Embedding：语义相似
- BM25：精确匹配
- 两者结合最优

## 人格关联

- **逻辑性**：理解检索机制
- **分析力**：性能优化

## 应用

- RAG 系统优化
- 知识库检索改进
