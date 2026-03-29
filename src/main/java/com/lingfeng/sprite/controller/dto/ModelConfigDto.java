package com.lingfeng.sprite.controller.dto;

public record ModelConfigDto(
        String provider,
        String modelName,
        String apiKey,
        String baseUrl,
        double temperature,
        int maxTokens
) {
    public ModelConfigDto {
        provider = provider == null || provider.isBlank() ? "minimax" : provider;
        modelName = modelName == null || modelName.isBlank() ? "MiniMax-Text-01" : modelName;
        apiKey = apiKey == null ? "" : apiKey;
        baseUrl = baseUrl == null || baseUrl.isBlank() ? "https://api.minimax.chat" : baseUrl;
        temperature = temperature <= 0 ? 0.7d : temperature;
        maxTokens = maxTokens <= 0 ? 4096 : maxTokens;
    }
}
