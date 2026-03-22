package com.lingfeng.sprite.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.lingfeng.sprite.MemorySystem;
import com.lingfeng.sprite.MemorySystem.LongTermMemory;

/**
 * 记忆持久化服务
 *
 * 负责将长期记忆保存到文件系统，并在启动时加载
 */
@Service
public class MemoryPersistenceService {

    private static final Logger logger = LoggerFactory.getLogger(MemoryPersistenceService.class);
    private static final String DATA_DIR = "data/memory";
    private static final String LONG_TERM_DIR = DATA_DIR + "/long-term";
    private static final long SAVE_INTERVAL_MINUTES = 30;

    private final MemorySystem.Memory memory;
    private final ObjectMapper objectMapper;
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private final ConcurrentHashMap<String, Instant> lastSaveTimes = new ConcurrentHashMap<>();

    public MemoryPersistenceService(@Autowired MemorySystem.Memory memory) {
        this.memory = memory;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
        this.objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
        this.objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        // 确保目录存在
        ensureDirectories();

        // 加载已有记忆
        loadMemory();

        // 启动定期保存
        startPeriodicSave();
    }

    private void ensureDirectories() {
        try {
            Files.createDirectories(Paths.get(LONG_TERM_DIR));
            logger.info("Memory persistence directory initialized: {}", LONG_TERM_DIR);
        } catch (IOException e) {
            logger.error("Failed to create memory persistence directory: {}", e.getMessage());
        }
    }

    /**
     * 加载记忆（启动时调用）
     */
    public void loadMemory() {
        try {
            LongTermMemory longTerm = memory.getLongTerm();

            // 加载情景记忆
            Path episodicFile = Paths.get(LONG_TERM_DIR, "episodic.json");
            if (Files.exists(episodicFile)) {
                MemorySystem.LongTermMemory.PersistedEpisodicList data =
                    objectMapper.readValue(episodicFile.toFile(), MemorySystem.LongTermMemory.PersistedEpisodicList.class);
                for (MemorySystem.EpisodicEntry entry : data.entries()) {
                    longTerm.storeEpisodic(entry);
                }
                logger.info("Loaded {} episodic memories", data.entries().size());
            }

            // 加载语义记忆
            Path semanticFile = Paths.get(LONG_TERM_DIR, "semantic.json");
            if (Files.exists(semanticFile)) {
                MemorySystem.LongTermMemory.PersistedSemanticList data =
                    objectMapper.readValue(semanticFile.toFile(), MemorySystem.LongTermMemory.PersistedSemanticList.class);
                for (MemorySystem.SemanticEntry entry : data.entries()) {
                    longTerm.storeSemantic(entry);
                }
                logger.info("Loaded {} semantic memories", data.entries().size());
            }

            // 加载程序记忆
            Path proceduralFile = Paths.get(LONG_TERM_DIR, "procedural.json");
            if (Files.exists(proceduralFile)) {
                MemorySystem.LongTermMemory.PersistedProceduralList data =
                    objectMapper.readValue(proceduralFile.toFile(), MemorySystem.LongTermMemory.PersistedProceduralList.class);
                for (MemorySystem.ProceduralEntry entry : data.entries()) {
                    longTerm.storeProcedural(entry);
                }
                logger.info("Loaded {} procedural memories", data.entries().size());
            }

            logger.info("Memory loaded successfully");
        } catch (Exception e) {
            logger.error("Failed to load memory: {}", e.getMessage(), e);
        }
    }

    /**
     * 保存记忆
     */
    public void saveMemory() {
        try {
            LongTermMemory longTerm = memory.getLongTerm();
            Instant now = Instant.now();

            // 保存情景记忆
            MemorySystem.LongTermMemory.PersistedEpisodicList episodicData =
                new MemorySystem.LongTermMemory.PersistedEpisodicList(longTerm.getAllEpisodic());
            objectMapper.writeValue(Paths.get(LONG_TERM_DIR, "episodic.json").toFile(), episodicData);

            // 保存语义记忆
            MemorySystem.LongTermMemory.PersistedSemanticList semanticData =
                new MemorySystem.LongTermMemory.PersistedSemanticList(longTerm.getAllSemantic());
            objectMapper.writeValue(Paths.get(LONG_TERM_DIR, "semantic.json").toFile(), semanticData);

            // 保存程序记忆
            MemorySystem.LongTermMemory.PersistedProceduralList proceduralData =
                new MemorySystem.LongTermMemory.PersistedProceduralList(longTerm.getAllProcedural());
            objectMapper.writeValue(Paths.get(LONG_TERM_DIR, "procedural.json").toFile(), proceduralData);

            lastSaveTimes.put("lastSave", now);
            logger.info("Memory saved successfully at {}", DateTimeFormatter.ISO_INSTANT.format(now));
        } catch (Exception e) {
            logger.error("Failed to save memory: {}", e.getMessage(), e);
        }
    }

    /**
     * 启动定期保存
     */
    private void startPeriodicSave() {
        scheduler.scheduleAtFixedRate(
            this::saveMemory,
            SAVE_INTERVAL_MINUTES,
            SAVE_INTERVAL_MINUTES,
            TimeUnit.MINUTES
        );
        logger.info("Periodic memory save started (every {} minutes)", SAVE_INTERVAL_MINUTES);
    }

    /**
     * 手动触发保存
     */
    public void forceSave() {
        saveMemory();
    }

    /**
     * 获取上次保存时间
     */
    public Instant getLastSaveTime() {
        return lastSaveTimes.get("lastSave");
    }

    /**
     * 关闭时保存
     */
    public void shutdown() {
        scheduler.shutdown();
        saveMemory();
        logger.info("MemoryPersistenceService shutdown complete");
    }
}
