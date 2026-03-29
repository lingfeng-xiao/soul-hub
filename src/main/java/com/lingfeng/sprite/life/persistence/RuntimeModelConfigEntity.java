package com.lingfeng.sprite.life.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "runtime_model_config")
public class RuntimeModelConfigEntity {

    @Id
    private Long id;

    @Column(nullable = false)
    private String provider;

    @Column(name = "model_name", nullable = false)
    private String modelName;

    @Column(name = "api_key", length = 2048)
    private String apiKey;

    @Column(name = "base_url", length = 2048)
    private String baseUrl;

    @Column(name = "temperature_value")
    private double temperature;

    @Column(name = "max_tokens")
    private int maxTokens;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public static RuntimeModelConfigEntity defaults() {
        RuntimeModelConfigEntity entity = new RuntimeModelConfigEntity();
        entity.id = 1L;
        entity.provider = "minimax";
        entity.modelName = "MiniMax-Text-01";
        entity.apiKey = "";
        entity.baseUrl = "https://api.minimax.chat";
        entity.temperature = 0.7d;
        entity.maxTokens = 4096;
        entity.updatedAt = Instant.now();
        return entity;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getProvider() { return provider; }
    public void setProvider(String provider) { this.provider = provider; }
    public String getModelName() { return modelName; }
    public void setModelName(String modelName) { this.modelName = modelName; }
    public String getApiKey() { return apiKey; }
    public void setApiKey(String apiKey) { this.apiKey = apiKey; }
    public String getBaseUrl() { return baseUrl; }
    public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }
    public double getTemperature() { return temperature; }
    public void setTemperature(double temperature) { this.temperature = temperature; }
    public int getMaxTokens() { return maxTokens; }
    public void setMaxTokens(int maxTokens) { this.maxTokens = maxTokens; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
