package com.lingfeng.sprite.life;

import com.lingfeng.sprite.config.AppConfig;
import com.lingfeng.sprite.controller.dto.ModelConfigDto;
import com.lingfeng.sprite.controller.dto.ModelConnectionTestResult;
import com.lingfeng.sprite.life.persistence.RuntimeModelConfigEntity;
import com.lingfeng.sprite.life.persistence.RuntimeModelConfigRepository;
import org.springframework.stereotype.Service;

import java.net.HttpURLConnection;
import java.net.URL;
import java.time.Instant;

@Service
public class ModelConfigService {

    private static final long CONFIG_ID = 1L;

    private final RuntimeModelConfigRepository repository;
    private final AppConfig appConfig;
    private final LifeJournalService lifeJournalService;

    public ModelConfigService(
            RuntimeModelConfigRepository repository,
            AppConfig appConfig,
            LifeJournalService lifeJournalService
    ) {
        this.repository = repository;
        this.appConfig = appConfig;
        this.lifeJournalService = lifeJournalService;
    }

    public ModelConfigDto getConfig() {
        return toDto(getOrCreate());
    }

    public ModelConfigDto update(ModelConfigDto config) {
        RuntimeModelConfigEntity entity = getOrCreate();
        entity.setProvider(config.provider());
        entity.setModelName(config.modelName());
        entity.setApiKey(config.apiKey());
        entity.setBaseUrl(config.baseUrl());
        entity.setTemperature(config.temperature());
        entity.setMaxTokens(config.maxTokens());
        entity.setUpdatedAt(Instant.now());
        RuntimeModelConfigEntity saved = repository.save(entity);
        applyToRuntimeConfig(saved);
        lifeJournalService.record("MODEL", "Model configuration updated", "The runtime model configuration was saved.", saved);
        return toDto(saved);
    }

    public ModelConnectionTestResult testConnection() {
        RuntimeModelConfigEntity entity = getOrCreate();
        if (entity.getApiKey() == null || entity.getApiKey().isBlank() || "default-key".equals(entity.getApiKey())) {
            return new ModelConnectionTestResult(false, "API key is empty or still using the default placeholder.");
        }

        String baseUrl = entity.getBaseUrl() == null ? "" : entity.getBaseUrl().trim();
        if (baseUrl.isBlank()) {
            return new ModelConnectionTestResult(false, "Base URL is empty.");
        }

        ModelConnectionTestResult headResult = probe(baseUrl, "HEAD");
        if (headResult.success()) {
            return headResult;
        }

        ModelConnectionTestResult getResult = probe(baseUrl, "GET");
        if (getResult.success()) {
            return getResult;
        }

        return new ModelConnectionTestResult(
                false,
                "Connection test failed with HEAD and GET: " + headResult.message() + " | " + getResult.message()
        );
    }

    private ModelConnectionTestResult probe(String baseUrl, String method) {
        try {
            HttpURLConnection connection = (HttpURLConnection) new URL(baseUrl).openConnection();
            connection.setRequestMethod(method);
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);
            connection.setInstanceFollowRedirects(true);
            int statusCode = connection.getResponseCode();
            if (statusCode > 0) {
                return new ModelConnectionTestResult(true, method + " endpoint reachable (HTTP " + statusCode + ").");
            }
            return new ModelConnectionTestResult(false, method + " endpoint did not return a valid status code.");
        } catch (Exception exception) {
            return new ModelConnectionTestResult(false, method + " probe failed: " + exception.getMessage());
        }
    }

    private RuntimeModelConfigEntity getOrCreate() {
        return repository.findById(CONFIG_ID).orElseGet(() -> {
            RuntimeModelConfigEntity entity = RuntimeModelConfigEntity.defaults();
            entity.setId(CONFIG_ID);
            applyFromAppConfig(entity);
            return repository.save(entity);
        });
    }

    private void applyFromAppConfig(RuntimeModelConfigEntity entity) {
        entity.setProvider("minimax");
        entity.setModelName("MiniMax-Text-01");
        entity.setApiKey(appConfig.getLlm().getMinmax().getApiKey());
        entity.setBaseUrl(appConfig.getLlm().getMinmax().getBaseUrl());
        entity.setTemperature(0.7d);
        entity.setMaxTokens(4096);
        entity.setUpdatedAt(Instant.now());
    }

    private void applyToRuntimeConfig(RuntimeModelConfigEntity entity) {
        appConfig.getLlm().setEnabled(entity.getApiKey() != null && !entity.getApiKey().isBlank());
        appConfig.getLlm().getMinmax().setApiKey(entity.getApiKey());
        appConfig.getLlm().getMinmax().setBaseUrl(entity.getBaseUrl());
    }

    private ModelConfigDto toDto(RuntimeModelConfigEntity entity) {
        return new ModelConfigDto(
                entity.getProvider(),
                entity.getModelName(),
                entity.getApiKey(),
                entity.getBaseUrl(),
                entity.getTemperature(),
                entity.getMaxTokens()
        );
    }
}
