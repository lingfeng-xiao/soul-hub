package com.lingfeng.sprite.config;

/**
 * 模型配置记录
 * 用于运行时更新模型配置
 */
public record ModelConfig(
    String provider,      // 提供商: minimax, openai, anthropic, azure
    String modelName,     // 模型名称
    String apiKey,        // API密钥
    String baseUrl,       // API基础URL
    float temperature,    // 温度参数
    int maxTokens         // 最大令牌数
) {
    public ModelConfig {
        if (provider == null) provider = "minimax";
        if (modelName == null) modelName = "MiniMax-Text-01";
        if (apiKey == null) apiKey = "";
        if (baseUrl == null) baseUrl = "https://api.minimax.chat/v1";
        if (temperature == 0) temperature = 0.7f;
        if (maxTokens == 0) maxTokens = 4096;
    }
}
