package com.lingfeng.sprite.service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.lingfeng.sprite.MemorySystem;
import com.lingfeng.sprite.MemorySystem.*;

/**
 * S10-2: 记忆可视化服务 - S12-1: 连接真实MemorySystem数据
 *
 * 提供记忆系统的可视化数据：
 * - 记忆分布统计
 * - 记忆强度分布
 * - 记忆类型分布
 * - 记忆活跃度分析
 */
public class MemoryVisualizationService {

    private static final Logger logger = LoggerFactory.getLogger(MemoryVisualizationService.class);

    public MemoryVisualizationService() {}

    /**
     * 记忆类型统计
     */
    public record MemoryTypeStats(
        int episodicCount,
        int semanticCount,
        int proceduralCount,
        int perceptiveCount,
        int workingMemoryCount
    ) {}

    /**
     * 记忆强度分布
     */
    public record StrengthDistribution(
        int veryLowCount,    // 0-0.2
        int lowCount,        // 0.2-0.4
        int mediumCount,     // 0.4-0.6
        int highCount,       // 0.6-0.8
        int veryHighCount    // 0.8-1.0
    ) {}

    /**
     * 记忆活跃度
     */
    public record MemoryActivity(
        String memoryId,
        String memoryType,
        Instant lastAccessed,
        int accessCount,
        float strength,
        String preview
    ) {}

    /**
     * 记忆可视化数据
     */
    public record MemoryVisualizationData(
        Instant timestamp,
        MemoryTypeStats typeStats,
        StrengthDistribution strengthDistribution,
        List<MemoryActivity> mostActiveMemories,
        List<MemoryActivity> weakestMemories,
        int totalMemoryCount,
        float averageStrength
    ) {}

    // ==================== 记忆分析接口 ====================

    /**
     * S12-1: 从真实MemorySystem获取可视化数据
     * 直接连接MemorySystem.Memory，获取真实的记忆数据
     */
    public MemoryVisualizationData generateVisualization(MemorySystem.Memory memory) {
        if (memory == null) {
            return createEmptyVisualization();
        }

        Instant now = Instant.now();

        // 获取各类型记忆数量
        MemorySystem.LongTermMemory ltm = memory.getLongTerm();
        MemorySystem.WorkingMemory wm = memory.getWorking();

        int episodicCount = ltm.getAllEpisodic().size();
        int semanticCount = ltm.getAllSemantic().size();
        int proceduralCount = ltm.getAllProcedural().size();
        int perceptiveCount = ltm.getAllPerceptive().size();
        int workingCount = wm.getAll().size();

        MemoryTypeStats typeStats = new MemoryTypeStats(
            episodicCount, semanticCount, proceduralCount, perceptiveCount, workingCount
        );

        // 计算强度分布（从程序记忆和感知记忆中获取强度数据）
        StrengthDistribution strengthDist = calculateStrengthDistribution(ltm);

        // 获取活跃记忆（从工作记忆和感知记忆中获取）
        List<MemoryActivity> mostActive = getMostActiveMemories(ltm, wm);
        List<MemoryActivity> weakest = getWeakestMemories(ltm, wm);

        int total = episodicCount + semanticCount + proceduralCount + perceptiveCount;
        float avgStrength = calculateAverageStrength(ltm, wm);

        return new MemoryVisualizationData(
            now,
            typeStats,
            strengthDist,
            mostActive,
            weakest,
            total,
            avgStrength
        );
    }

    /**
     * S12-1: 保留旧接口以兼容（使用泛型列表）
     * @deprecated 使用 {@link #generateVisualization(Memory)} 替代
     */
    @Deprecated
    public MemoryVisualizationData generateVisualization(
        List<?> episodicMemories,
        List<?> semanticMemories,
        List<?> proceduralMemories,
        List<?> perceptiveMemories,
        List<?> workingMemories
    ) {
        // 尝试转换为正确的类型并调用真实方法
        MemorySystem.Memory memory = createMemoryFromLists(
            episodicMemories, semanticMemories, proceduralMemories, perceptiveMemories, workingMemories
        );
        if (memory != null) {
            return generateVisualization(memory);
        }
        return createEmptyVisualization();
    }

    /**
     * 从列表创建Memory对象（仅用于兼容旧接口）
     */
    @SuppressWarnings("unchecked")
    private MemorySystem.Memory createMemoryFromLists(
        List<?> episodic, List<?> semantic, List<?> procedural,
        List<?> perceptive, List<?> working
    ) {
        try {
            // 检查是否为正确类型
            if (episodic == null || !(episodic.isEmpty() || episodic.get(0) instanceof EpisodicEntry)) {
                return null;
            }

            MemorySystem.LongTermMemory ltm = new MemorySystem.LongTermMemory();
            for (EpisodicEntry e : (List<EpisodicEntry>) episodic) {
                ltm.storeEpisodic(e);
            }
            for (SemanticEntry e : (List<SemanticEntry>) semantic) {
                ltm.storeSemantic(e);
            }
            for (ProceduralEntry e : (List<ProceduralEntry>) procedural) {
                ltm.storeProcedural(e);
            }
            for (PerceptiveEntry e : (List<PerceptiveEntry>) perceptive) {
                ltm.storePerceptive(e);
            }

            MemorySystem.SensoryMemory sensory = new MemorySystem.SensoryMemory();
            MemorySystem.WorkingMemory wm = new MemorySystem.WorkingMemory();
            for (Object o : working) {
                if (o instanceof WorkingMemoryItem item) {
                    wm.add(item);
                }
            }

            return new MemorySystem.Memory(sensory, wm, ltm);
        } catch (Exception e) {
            logger.debug("Failed to convert lists to Memory: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 计算强度分布
     */
    private StrengthDistribution calculateStrengthDistribution(LongTermMemory ltm) {
        int veryLow = 0, low = 0, medium = 0, high = 0, veryHigh = 0;

        // 从程序记忆中获取强度（successRate）
        for (ProceduralEntry p : ltm.getAllProcedural()) {
            float strength = p.successRate();
            if (strength < 0.2f) veryLow++;
            else if (strength < 0.4f) low++;
            else if (strength < 0.6f) medium++;
            else if (strength < 0.8f) high++;
            else veryHigh++;
        }

        // 从感知记忆中获取强度
        for (PerceptiveEntry p : ltm.getAllPerceptive()) {
            float strength = p.strength();
            if (strength < 0.2f) veryLow++;
            else if (strength < 0.4f) low++;
            else if (strength < 0.6f) medium++;
            else if (strength < 0.8f) high++;
            else veryHigh++;
        }

        // 从语义记忆中获取置信度作为强度
        for (SemanticEntry s : ltm.getAllSemantic()) {
            float strength = s.confidence();
            if (strength < 0.2f) veryLow++;
            else if (strength < 0.4f) low++;
            else if (strength < 0.6f) medium++;
            else if (strength < 0.8f) high++;
            else veryHigh++;
        }

        return new StrengthDistribution(veryLow, low, medium, high, veryHigh);
    }

    /**
     * 获取最活跃的记忆
     */
    private List<MemoryActivity> getMostActiveMemories(LongTermMemory ltm, WorkingMemory wm) {
        List<MemoryActivity> activities = new ArrayList<>();

        // 从工作记忆获取活跃度（按访问次数排序）
        for (WorkingMemoryItem item : wm.getAll()) {
            String preview = item.content().toString();
            if (preview.length() > 50) {
                preview = preview.substring(0, 50) + "...";
            }
            activities.add(new MemoryActivity(
                item.id(),
                "WORKING",
                item.lastAccessed(),
                item.accessCount(),
                item.relevance(),
                preview
            ));
        }

        // 从感知记忆获取活跃度（按触发次数）
        for (PerceptiveEntry p : ltm.getAllPerceptive()) {
            activities.add(new MemoryActivity(
                p.id(),
                "PERCEPTIVE",
                Instant.now(),  // 感知记忆没有lastAccessed
                p.timesTriggered(),
                p.strength(),
                p.pattern() + " -> " + p.association()
            ));
        }

        // 按访问次数排序，取前10个
        return activities.stream()
            .sorted((a, b) -> Integer.compare(b.accessCount(), a.accessCount()))
            .limit(10)
            .toList();
    }

    /**
     * 获取最弱的记忆（低强度/低访问）
     */
    private List<MemoryActivity> getWeakestMemories(LongTermMemory ltm, WorkingMemory wm) {
        List<MemoryActivity> activities = new ArrayList<>();

        // 工作记忆按相关性排序
        for (WorkingMemoryItem item : wm.getAll()) {
            String preview = item.content().toString();
            if (preview.length() > 50) {
                preview = preview.substring(0, 50) + "...";
            }
            activities.add(new MemoryActivity(
                item.id(),
                "WORKING",
                item.lastAccessed(),
                item.accessCount(),
                item.relevance(),
                preview
            ));
        }

        // 感知记忆按强度排序
        for (PerceptiveEntry p : ltm.getAllPerceptive()) {
            activities.add(new MemoryActivity(
                p.id(),
                "PERCEPTIVE",
                Instant.now(),
                p.timesTriggered(),
                p.strength(),
                p.pattern() + " -> " + p.association()
            ));
        }

        // 按强度排序，取前10个最弱的
        return activities.stream()
            .sorted((a, b) -> Float.compare(a.strength(), b.strength()))
            .limit(10)
            .toList();
    }

    /**
     * 计算平均强度
     */
    private float calculateAverageStrength(LongTermMemory ltm, WorkingMemory wm) {
        float totalStrength = 0;
        int count = 0;

        for (WorkingMemoryItem item : wm.getAll()) {
            totalStrength += item.relevance();
            count++;
        }

        for (ProceduralEntry p : ltm.getAllProcedural()) {
            totalStrength += p.successRate();
            count++;
        }

        for (PerceptiveEntry p : ltm.getAllPerceptive()) {
            totalStrength += p.strength();
            count++;
        }

        for (SemanticEntry s : ltm.getAllSemantic()) {
            totalStrength += s.confidence();
            count++;
        }

        return count > 0 ? totalStrength / count : 0.5f;
    }

    /**
     * 创建空的可视化数据
     */
    private MemoryVisualizationData createEmptyVisualization() {
        Instant now = Instant.now();
        MemoryTypeStats typeStats = new MemoryTypeStats(0, 0, 0, 0, 0);
        StrengthDistribution strengthDist = new StrengthDistribution(0, 0, 0, 0, 0);
        return new MemoryVisualizationData(
            now, typeStats, strengthDist,
            List.of(), List.of(), 0, 0.5f
        );
    }

    /**
     * 获取记忆类型描述
     */
    public String getMemoryTypeDescription(MemoryTypeStats stats) {
        int total = stats.episodicCount() + stats.semanticCount() +
                    stats.proceduralCount() + stats.perceptiveCount();

        if (total == 0) {
            return "No memories stored";
        }

        return String.format(
            "Total: %d memories (Episodic: %d, Semantic: %d, Procedural: %d, Perceptive: %d)",
            total,
            stats.episodicCount(),
            stats.semanticCount(),
            stats.proceduralCount(),
            stats.perceptiveCount()
        );
    }

    /**
     * 获取强度分布描述
     */
    public String getStrengthDistributionDescription(StrengthDistribution dist) {
        int total = dist.veryLowCount() + dist.lowCount() + dist.mediumCount() +
                    dist.highCount() + dist.veryHighCount();

        if (total == 0) {
            return "No memory strength data";
        }

        return String.format(
            "Strength Distribution: Very High(%d), High(%d), Medium(%d), Low(%d), Very Low(%d)",
            dist.veryHighCount(), dist.highCount(), dist.mediumCount(),
            dist.lowCount(), dist.veryLowCount()
        );
    }

    /**
     * 记忆时间线数据（用于可视化）
     */
    public record MemoryTimeline(
        Instant startDate,
        Instant endDate,
        List<TimelineEntry> entries
    ) {
        public record TimelineEntry(
            Instant date,
            String memoryType,
            String description,
            float strength
        ) {}
    }

    /**
     * S12-1: 从真实MemorySystem生成记忆时间线
     */
    public MemoryTimeline generateTimeline(
        MemorySystem.Memory memory,
        Instant startDate,
        Instant endDate
    ) {
        Instant effectiveStart = startDate != null ? startDate : Instant.now().minus(30, ChronoUnit.DAYS);
        Instant effectiveEnd = endDate != null ? endDate : Instant.now();

        if (memory == null) {
            return new MemoryTimeline(effectiveStart, effectiveEnd, List.of());
        }

        List<TimelineEntry> entries = new ArrayList<>();

        // 从情景记忆获取时间线数据
        for (EpisodicEntry episodic : memory.getLongTerm().getAllEpisodic()) {
            if (episodic.timestamp().isAfter(effectiveStart) &&
                episodic.timestamp().isBefore(effectiveEnd)) {
                entries.add(new TimelineEntry(
                    episodic.timestamp(),
                    "EPISODIC",
                    episodic.experience(),
                    0.7f  // 情景记忆使用默认强度
                ));
            }
        }

        // 从程序记忆获取时间线数据
        for (ProceduralEntry procedural : memory.getLongTerm().getAllProcedural()) {
            if (procedural.lastPracticed() != null &&
                procedural.lastPracticed().isAfter(effectiveStart) &&
                procedural.lastPracticed().isBefore(effectiveEnd)) {
                entries.add(new TimelineEntry(
                    procedural.lastPracticed(),
                    "PROCEDURAL",
                    procedural.skillName() + ": " + procedural.procedure(),
                    procedural.successRate()
                ));
            }
        }

        // 按日期排序
        entries.sort((a, b) -> b.date().compareTo(a.date()));

        return new MemoryTimeline(effectiveStart, effectiveEnd, entries);
    }

    /**
     * @deprecated 使用 {@link #generateTimeline(Memory, Instant, Instant)} 替代
     */
    @Deprecated
    public MemoryTimeline generateTimeline(
        List<?> episodicMemories,
        Instant startDate,
        Instant endDate
    ) {
        MemorySystem.Memory memory = createMemoryFromLists(
            episodicMemories, List.of(), List.of(), List.of(), List.of()
        );
        if (memory != null) {
            return generateTimeline(memory, startDate, endDate);
        }
        return new MemoryTimeline(
            startDate != null ? startDate : Instant.now().minus(30, ChronoUnit.DAYS),
            endDate != null ? endDate : Instant.now(),
            List.of()
        );
    }
}
