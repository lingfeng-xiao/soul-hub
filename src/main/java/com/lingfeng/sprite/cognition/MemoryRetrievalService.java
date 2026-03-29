package com.lingfeng.sprite.cognition;

import com.lingfeng.sprite.MemorySystem;
import com.lingfeng.sprite.MemorySystem.*;
import com.lingfeng.sprite.OwnerModel;
import com.lingfeng.sprite.PerceptionSystem;
import com.lingfeng.sprite.WorldModel;
import com.lingfeng.sprite.service.VectorStore;
import com.lingfeng.sprite.service.KnowledgeGraph;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 记忆检索服务 - 为推理引擎提供长期记忆上下文
 *
 * 核心职责：
 * 1. 根据当前情境检索相关的情景记忆（past experiences）
 * 2. 检索相关的语义记忆（knowledge concepts）
 * 3. 检索相关的程序记忆（skill procedures）
 * 4. 检索相关的感知记忆（pattern associations）
 * 5. 将检索结果格式化为推理上下文的一部分
 *
 * ## 检索策略
 *
 * - 基于活动类型（WORK/LEISURE）检索相关记忆
 * - 基于时间上下文（早/中/晚）检索时间模式
 * - 基于情绪状态检索相似情绪下的成功经验
 * - 实现记忆相关性评分和排序
 */
public class MemoryRetrievalService {

    private final MemorySystem.Memory memory;
    private final ZoneId timezone = ZoneId.of("Asia/Shanghai");
    private final VectorStore vectorStore;
    private final KnowledgeGraph knowledgeGraph;

    // S20-3: Cross-modal memory associations (memoryId1 -> memoryId2 -> strength)
    private final Map<String, Map<String, Float>> crossModalAssociations = new HashMap<>();

    // 记忆检索结果
    public record RetrievalContext(
        List<String> relevantEpisodic,     // 相关情景记忆描述
        List<String> relevantSemantic,     // 相关语义记忆描述
        List<String> relevantProcedural,   // 相关程序记忆描述
        List<String> relevantPatterns,     // 相关感知模式
        float overallRelevance            // 整体相关性评分
    ) {
        public boolean isEmpty() {
            return relevantEpisodic.isEmpty()
                && relevantSemantic.isEmpty()
                && relevantProcedural.isEmpty()
                && relevantPatterns.isEmpty();
        }
    }

    // S20-4: 召回记忆结构 (用于 ReasoningFrame 和 DecisionRationale)
    public record RecalledMemory(
        String memoryId,         // 记忆ID
        String memoryType,       // 记忆类型: EPISODIC, SEMANTIC, PROCEDURAL, PERCEPTIVE
        String content,          // 记忆内容摘要
        float relevanceScore,    // 相关性评分
        Instant recalledAt       // 召回时间
    ) {}

    public MemoryRetrievalService(MemorySystem.Memory memory) {
        this.memory = memory;
        this.vectorStore = new VectorStore();
        this.knowledgeGraph = new KnowledgeGraph();
    }

    public MemoryRetrievalService(MemorySystem.Memory memory, VectorStore vectorStore, KnowledgeGraph knowledgeGraph) {
        this.memory = memory;
        this.vectorStore = vectorStore;
        this.knowledgeGraph = knowledgeGraph;
    }

    // ==================== S20-1: Vector Similarity Search ====================

    /**
     * S20-1: 使用向量嵌入进行语义相似度搜索
     *
     * @param queryText 查询文本
     * @param topK 返回结果数量
     * @return 相似记忆列表
     */
    public List<String> vectorSearch(String queryText, int topK) {
        if (queryText == null || queryText.isEmpty()) {
            return List.of();
        }
        float[] queryEmbedding = vectorStore.generateTextEmbedding(queryText);
        return vectorStore.search(queryEmbedding, topK);
    }

    /**
     * S20-1: 使用向量嵌入搜索并返回带分数的结果
     *
     * @param queryText 查询文本
     * @param topK 返回结果数量
     * @return 带相似度分数的搜索结果
     */
    public List<VectorStore.SearchResult> vectorSearchWithScores(String queryText, int topK) {
        if (queryText == null || queryText.isEmpty()) {
            return List.of();
        }
        float[] queryEmbedding = vectorStore.generateTextEmbedding(queryText);
        return vectorStore.searchWithScores(queryEmbedding, topK);
    }

    /**
     * S20-1: 将记忆存储到向量库
     *
     * @param memoryId 记忆ID
     * @param content 记忆内容
     * @param memoryType 记忆类型
     */
    public void storeToVector(String memoryId, String content, String memoryType) {
        float[] embedding = vectorStore.generateTextEmbedding(content);
        vectorStore.store(memoryId, embedding, memoryType, content);
    }

    /**
     * S20-1: 获取向量存储
     */
    public VectorStore getVectorStore() {
        return vectorStore;
    }

    // ==================== S20-2: Knowledge Graph ====================

    /**
     * S20-2: 获取知识图谱
     */
    public KnowledgeGraph getKnowledgeGraph() {
        return knowledgeGraph;
    }

    /**
     * S20-2: 添加实体到知识图谱
     *
     * @param name 实体名称
     * @param type 实体类型
     */
    public void addEntity(String name, String type) {
        knowledgeGraph.addEntity(name, type);
    }

    /**
     * S20-2: 添加实体关系
     *
     * @param entity1 实体1
     * @param relation 关系类型
     * @param entity2 实体2
     * @param strength 关系强度
     */
    public void addRelation(String entity1, String relation, String entity2, float strength) {
        knowledgeGraph.addRelation(entity1, relation, entity2, strength);
    }

    /**
     * S20-2: 获取与实体相关的所有实体
     *
     * @param entity 实体名称
     * @return 关系列表
     */
    public List<KnowledgeGraph.Relation> getRelatedEntities(String entity) {
        return knowledgeGraph.getRelated(entity);
    }

    // ==================== S20-3: Cross-modal Memory Association ====================

    /**
     * S20-3: 建立跨模态记忆关联
     * 关联不同类型的记忆：情景 <-> 语义 <-> 程序 <-> 感知
     *
     * @param memoryId1 第一个记忆ID
     * @param memoryId2 第二个记忆ID
     * @param strength 关联强度 (0-1)
     */
    public void associateMemories(String memoryId1, String memoryId2, float strength) {
        if (memoryId1 == null || memoryId2 == null || memoryId1.equals(memoryId2)) {
            return;
        }
        strength = Math.max(0f, Math.min(1f, strength));

        // 双向关联
        crossModalAssociations
            .computeIfAbsent(memoryId1, k -> new HashMap<>())
            .put(memoryId2, strength);
        crossModalAssociations
            .computeIfAbsent(memoryId2, k -> new HashMap<>())
            .put(memoryId1, strength);
    }

    /**
     * S20-3: 获取与指定记忆关联的所有记忆
     *
     * @param memoryId 记忆ID
     * @return 关联的记忆ID及强度
     */
    public Map<String, Float> getAssociatedMemories(String memoryId) {
        return crossModalAssociations.getOrDefault(memoryId, Map.of());
    }

    /**
     * S20-3: 获取跨模态关联记忆的内容
     *
     * @param memoryId 记忆ID
     * @return 关联记忆的描述
     */
    public List<String> getAssociatedMemoryContents(String memoryId) {
        Map<String, Float> associations = getAssociatedMemories(memoryId);
        List<String> contents = new ArrayList<>();

        for (Map.Entry<String, Float> entry : associations.entrySet()) {
            String otherId = entry.getKey();
            float strength = entry.getValue();

            // 从向量存储获取内容
            VectorStore.VectorMetadata meta = vectorStore.getMetadata(otherId);
            if (meta != null && meta.content() != null && !meta.content().isEmpty()) {
                contents.add(String.format("[%.0f%%相关] %s",
                    strength * 100, meta.content()));
            }
        }

        return contents;
    }

    /**
     * S20-3: 清除记忆关联
     *
     * @param memoryId 记忆ID
     */
    public void clearMemoryAssociations(String memoryId) {
        crossModalAssociations.remove(memoryId);
        // Also remove from other memories
        for (Map<String, Float> others : crossModalAssociations.values()) {
            others.remove(memoryId);
        }
    }

    // ==================== S20-4: Proactive Memory Recommendation ====================

    /**
     * S20-4: 基于当前感知主动推荐相关记忆
     *
     * @param currentPerception 当前感知
     * @return 推荐的记忆列表
     */
    public List<MemoryRecommendation> recommendRelatedMemories(PerceptionSystem.Perception currentPerception) {
        if (currentPerception == null) {
            return List.of();
        }

        List<MemoryRecommendation> recommendations = new ArrayList<>();

        // 1. 基于用户活动的推荐
        if (currentPerception.user() != null) {
            String activity = currentPerception.user().currentActivity();
            if (activity != null && !activity.isEmpty()) {
                recommendations.addAll(recommendByActivity(activity));
            }
        }

        // 2. 基于环境的推荐
        if (currentPerception.environment() != null) {
            String location = currentPerception.environment().location();
            if (location != null && !location.isEmpty()) {
                recommendations.addAll(recommendByLocation(location));
            }
        }

        // 3. 基于时间的推荐
        recommendations.addAll(recommendByTimeContext());

        // 4. 基于情绪的推荐
        if (currentPerception.user() != null && currentPerception.user().mood() != null) {
            recommendations.addAll(recommendByMood(currentPerception.user().mood()));
        }

        // 去重并排序
        return recommendations.stream()
            .collect(Collectors.toMap(MemoryRecommendation::memoryId, r -> r, (a, b) -> a))
            .values()
            .stream()
            .sorted((a, b) -> Float.compare(b.relevanceScore(), a.relevanceScore()))
            .limit(10)
            .collect(Collectors.toList());
    }

    /**
     * 基于活动的推荐
     */
    private List<MemoryRecommendation> recommendByActivity(String activity) {
        List<MemoryRecommendation> results = new ArrayList<>();

        // 语义搜索
        List<VectorStore.SearchResult> vectorResults = vectorStore.searchWithScores(
            vectorStore.generateTextEmbedding(activity), 5);

        for (VectorStore.SearchResult result : vectorResults) {
            if (result.metadata() != null) {
                results.add(new MemoryRecommendation(
                    result.id(),
                    result.metadata().content(),
                    "活动相关",
                    result.score()
                ));
            }
        }

        // 知识图谱关联
        List<KnowledgeGraph.Relation> related = knowledgeGraph.getRelated(activity);
        for (KnowledgeGraph.Relation r : related) {
            results.add(new MemoryRecommendation(
                r.entity2(),
                r.entity2(),
                "知识关联: " + r.relation(),
                r.strength()
            ));
        }

        return results;
    }

    /**
     * 基于位置的推荐
     */
    private List<MemoryRecommendation> recommendByLocation(String location) {
        List<MemoryRecommendation> results = new ArrayList<>();

        if (memory != null && memory.getLongTerm() != null) {
            List<SemanticEntry> semantic = memory.getLongTerm().recallSemantic(location);
            for (SemanticEntry entry : semantic) {
                results.add(new MemoryRecommendation(
                    entry.id(),
                    entry.concept(),
                    "位置相关",
                    0.7f
                ));
            }
        }

        return results;
    }

    /**
     * 基于时间上下文的推荐
     */
    private List<MemoryRecommendation> recommendByTimeContext() {
        List<MemoryRecommendation> results = new ArrayList<>();
        String timeContext = getTimeContext();

        if (memory != null && memory.getLongTerm() != null) {
            List<EpisodicEntry> episodic = memory.getLongTerm().recallEpisodic(timeContext, 3);
            for (EpisodicEntry entry : episodic) {
                results.add(new MemoryRecommendation(
                    entry.id(),
                    entry.experience(),
                    "同时段记忆",
                    0.5f
                ));
            }
        }

        return results;
    }

    /**
     * 基于情绪的推荐
     */
    private List<MemoryRecommendation> recommendByMood(String mood) {
        List<MemoryRecommendation> results = new ArrayList<>();

        if (memory != null && memory.getLongTerm() != null) {
            List<EpisodicEntry> episodic = memory.getLongTerm().recallEpisodic(mood, 5);
            for (EpisodicEntry entry : episodic) {
                results.add(new MemoryRecommendation(
                    entry.id(),
                    entry.experience(),
                    "情绪共鸣",
                    0.6f
                ));
            }
        }

        return results;
    }

    /**
     * S20-4: 记忆推荐结果
     */
    public record MemoryRecommendation(
        String memoryId,
        String content,
        String reason,
        float relevanceScore
    ) {}

    /**
     * 根据当前情境检索相关记忆
     *
     * @param context 当前世界模型情境
     * @param mood 当前主人情绪
     * @return 检索到的记忆上下文
     */
    public RetrievalContext retrieve(WorldModel.Context context, OwnerModel.Mood mood) {
        if (memory == null || memory.getLongTerm() == null) {
            return emptyContext();
        }

        LongTermMemory longTerm = memory.getLongTerm();
        List<String> episodic = new ArrayList<>();
        List<String> semantic = new ArrayList<>();
        List<String> procedural = new ArrayList<>();
        List<String> patterns = new ArrayList<>();
        float totalRelevance = 0f;

        // 1. 基于活动类型检索情景记忆
        if (context != null && context.activity() != null) {
            String activityQuery = context.activity().name();
            List<EpisodicEntry> episodicResults = longTerm.recallEpisodic(activityQuery, 5);
            for (EpisodicEntry entry : episodicResults) {
                episodic.add(formatEpisodic(entry));
                totalRelevance += 0.3f;
            }

            // 检索同一活动的过去经历
            List<EpisodicEntry> recentSameActivity = longTerm.getRecentEpisodic(7).stream()
                .filter(e -> {
                    // 检查是否有相同的活动关键词
                    String exp = e.experience().toLowerCase();
                    return exp.contains(activityQuery.toLowerCase()) ||
                           (e.emotion() != null && e.emotion().contains(mood != null ? mood.name() : ""));
                })
                .limit(3)
                .toList();
            for (EpisodicEntry entry : recentSameActivity) {
                String formatted = formatEpisodic(entry);
                if (!episodic.contains(formatted)) {
                    episodic.add(formatted);
                    totalRelevance += 0.2f;
                }
            }
        }

        // 2. 基于情绪检索相似情绪下的记忆
        if (mood != null) {
            String moodStr = mood.name();
            List<EpisodicEntry> moodResults = longTerm.recallEpisodic(moodStr, 3);
            for (EpisodicEntry entry : moodResults) {
                String formatted = formatEpisodic(entry);
                if (!episodic.contains(formatted)) {
                    episodic.add("【类似情绪】" + formatted);
                    totalRelevance += 0.15f;
                }
            }
        }

        // 3. 基于时间上下文检索（早/中/晚）
        String timeContext = getTimeContext();
        if (!timeContext.isEmpty()) {
            List<EpisodicEntry> timeResults = longTerm.recallEpisodic(timeContext, 3);
            for (EpisodicEntry entry : timeResults) {
                String formatted = formatEpisodic(entry);
                if (!episodic.contains(formatted)) {
                    episodic.add("【同时段】" + formatted);
                    totalRelevance += 0.1f;
                }
            }
        }

        // 4. 检索感知模式（perceptive memory）
        if (context != null && context.activity() != null) {
            List<PerceptiveEntry> perceptiveResults = longTerm.recallPerceptive(context.activity().name());
            for (PerceptiveEntry entry : perceptiveResults) {
                patterns.add(formatPerceptive(entry));
                totalRelevance += 0.25f;
            }
        }

        // 5. 检索与当前情境相关的语义记忆
        if (context != null && context.location() != null) {
            List<SemanticEntry> semanticResults = longTerm.recallSemantic(context.location());
            for (SemanticEntry entry : semanticResults) {
                semantic.add(formatSemantic(entry));
                totalRelevance += 0.2f;
            }
        }

        // 限制结果数量，避免上下文过长
        return new RetrievalContext(
            episodic.stream().limit(5).toList(),
            semantic.stream().limit(3).toList(),
            procedural.stream().limit(2).toList(),
            patterns.stream().limit(3).toList(),
            Math.min(totalRelevance, 1.0f)
        );
    }

    /**
     * 检索与动作相关的程序记忆（技能）
     */
    public List<String> retrieveSkillsForAction(String actionType) {
        if (memory == null || memory.getLongTerm() == null) {
            return List.of();
        }

        LongTermMemory longTerm = memory.getLongTerm();
        ProceduralEntry procedural = longTerm.recallProcedural(actionType);

        if (procedural != null) {
            return List.of(formatProcedural(procedural));
        }
        return List.of();
    }

    /**
     * 检索最近的正面经验（用于情绪提振）
     */
    public List<String> retrievePositiveExperiences(int limit) {
        if (memory == null || memory.getLongTerm() == null) {
            return List.of();
        }

        LongTermMemory longTerm = memory.getLongTerm();
        List<EpisodicEntry> recent = longTerm.getRecentEpisodic(30); // 最近30天

        return recent.stream()
            .filter(e -> {
                String emotion = e.emotion() != null ? e.emotion().toLowerCase() : "";
                return emotion.contains("开心") || emotion.contains("满足") ||
                       emotion.contains("excited") || emotion.contains("happy");
            })
            .limit(limit)
            .map(this::formatEpisodic)
            .toList();
    }

    /**
     * 检索最近的负面经验（用于预警）
     */
    public List<String> retrieveNegativeExperiences(int limit) {
        if (memory == null || memory.getLongTerm() == null) {
            return List.of();
        }

        LongTermMemory longTerm = memory.getLongTerm();
        List<EpisodicEntry> recent = longTerm.getRecentEpisodic(30);

        return recent.stream()
            .filter(e -> {
                String emotion = e.emotion() != null ? e.emotion().toLowerCase() : "";
                return emotion.contains("焦虑") || emotion.contains("挫折") ||
                       emotion.contains("沮丧") || emotion.contains("anxious") ||
                       emotion.contains("frustrated");
            })
            .limit(limit)
            .map(this::formatEpisodic)
            .toList();
    }

    /**
     * 获取时间上下文描述
     */
    private String getTimeContext() {
        LocalDateTime now = LocalDateTime.now(timezone);
        int hour = now.getHour();

        if (hour >= 6 && hour < 9) {
            return "早上";
        } else if (hour >= 9 && hour < 12) {
            return "上午";
        } else if (hour >= 12 && hour < 14) {
            return "中午";
        } else if (hour >= 14 && hour < 18) {
            return "下午";
        } else if (hour >= 18 && hour < 22) {
            return "晚上";
        } else {
            return "深夜";
        }
    }

    /**
     * 格式化情景记忆为可读字符串
     */
    private String formatEpisodic(EpisodicEntry entry) {
        StringBuilder sb = new StringBuilder();
        sb.append("【").append(formatInstant(entry.timestamp())).append("】");
        sb.append(entry.experience());
        if (entry.lesson() != null && !entry.lesson().isEmpty()) {
            sb.append(" → 教训: ").append(entry.lesson());
        }
        return sb.toString();
    }

    /**
     * 格式化语义记忆为可读字符串
     */
    private String formatSemantic(SemanticEntry entry) {
        return "【知识】" + entry.concept() + ": " + entry.definition();
    }

    /**
     * 格式化程序记忆为可读字符串
     */
    private String formatProcedural(ProceduralEntry entry) {
        return "【技能." + entry.skillName() + "】" +
               "熟练度: " + entry.level() +
               " (执行" + entry.timesPerformed() + "次, 成功率" +
               String.format("%.0f%%", entry.successRate() * 100) + ")";
    }

    /**
     * 格式化感知记忆为可读字符串
     */
    private String formatPerceptive(PerceptiveEntry entry) {
        return "【模式】" + entry.trigger() + " → " + entry.pattern();
    }

    /**
     * 格式化时间戳
     */
    private String formatInstant(Instant instant) {
        if (instant == null) return "未知时间";
        LocalDateTime ldt = instant.atZone(timezone).toLocalDateTime();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM-dd HH:mm");
        return ldt.format(formatter);
    }

    /**
     * 返回空上下文
     */
    private RetrievalContext emptyContext() {
        return new RetrievalContext(List.of(), List.of(), List.of(), List.of(), 0f);
    }

    /**
     * 构建增强的推理上下文字符串
     */
    public String buildMemoryContextString(RetrievalContext context) {
        if (context.isEmpty()) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("\n## 相关记忆上下文\n");

        if (!context.relevantEpisodic().isEmpty()) {
            sb.append("【情景记忆】\n");
            context.relevantEpisodic().forEach(e -> sb.append("- ").append(e).append("\n"));
        }

        if (!context.relevantSemantic().isEmpty()) {
            sb.append("【语义记忆】\n");
            context.relevantSemantic().forEach(e -> sb.append("- ").append(e).append("\n"));
        }

        if (!context.relevantProcedural().isEmpty()) {
            sb.append("【程序记忆】\n");
            context.relevantProcedural().forEach(e -> sb.append("- ").append(e).append("\n"));
        }

        if (!context.relevantPatterns().isEmpty()) {
            sb.append("【感知模式】\n");
            context.relevantPatterns().forEach(e -> sb.append("- ").append(e).append("\n"));
        }

        sb.append("整体相关性: ").append(String.format("%.0f%%", context.overallRelevance() * 100)).append("\n");

        return sb.toString();
    }
}
