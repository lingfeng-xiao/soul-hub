package com.lingfeng.sprite.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.lingfeng.sprite.llm.MinMaxConfig;
import com.lingfeng.sprite.llm.MinMaxLlmReasoner;

/**
 * 模型配置服务
 *
 * 管理模型配置，支持运行时更新
 * 所有需要LLM的服务都应该从这里获取reasoner，而不是自己创建
 */
@Service
public class ModelConfigService {

    private static final Logger logger = LoggerFactory.getLogger(ModelConfigService.class);

    private volatile ModelConfig currentConfig;
    private volatile MinMaxLlmReasoner llmReasoner;
    private final AppConfig appConfig;

    public ModelConfigService(AppConfig appConfig) {
        this.appConfig = appConfig;
        // Initialize with app config values from application.yml
        String apiKey = appConfig.getLlm().getMinmax().getApiKey();
        String baseUrl = appConfig.getLlm().getMinmax().getBaseUrl();

        this.currentConfig = new ModelConfig(
            "minimax",
            "MiniMax-Text-01",
            apiKey != null ? apiKey : "",
            baseUrl != null ? baseUrl : "https://api.minimax.chat/v1",
            0.7f,
            4096
        );
        this.llmReasoner = createLlmReasoner(this.currentConfig);
        logger.info("ModelConfigService initialized with provider: {}, model: {}, apiKey: {}",
            currentConfig.provider(), currentConfig.modelName(),
            currentConfig.apiKey() != null && !currentConfig.apiKey().isEmpty()
                ? currentConfig.apiKey().substring(0, Math.min(10, currentConfig.apiKey().length())) + "..."
                : "NOT SET");
    }

    public ModelConfig getConfig() {
        return currentConfig;
    }

    public synchronized ModelConfig updateConfig(ModelConfig newConfig) {
        logger.info("Updating model config: provider={}, model={}, baseUrl={}",
            newConfig.provider(), newConfig.modelName(), newConfig.baseUrl());

        this.currentConfig = newConfig;
        this.llmReasoner = createLlmReasoner(newConfig);

        logger.info("Model config updated successfully");
        return currentConfig;
    }

    /**
     * 获取当前的 LLM Reasoner
     * 每次调用返回同一个实例，除非配置被更新
     */
    public MinMaxLlmReasoner getLlmReasoner() {
        return llmReasoner;
    }

    /**
     * 测试连接 - 简单检查reasoner是否可用
     */
    public boolean testConnection() {
        try {
            if (llmReasoner == null) {
                logger.warn("LLM reasoner is null, cannot test connection");
                return false;
            }
            logger.info("Testing model connection with provider: {}, model: {}",
                currentConfig.provider(), currentConfig.modelName());
            return llmReasoner != null;
        } catch (Exception e) {
            logger.error("Model connection test failed: {}", e.getMessage());
            return false;
        }
    }

    private MinMaxLlmReasoner createLlmReasoner(ModelConfig config) {
        MinMaxConfig minmaxConfig = new MinMaxConfig(
            config.apiKey(),
            config.baseUrl(),
            config.modelName()
        );
        return new MinMaxLlmReasoner(minmaxConfig);
    }
}
