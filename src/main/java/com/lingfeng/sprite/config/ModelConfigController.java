package com.lingfeng.sprite.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 模型配置 REST API 控制器
 *
 * 提供模型配置的查询和更新接口
 */
@RestController
@RequestMapping("/api/model")
public class ModelConfigController {

    private static final Logger logger = LoggerFactory.getLogger(ModelConfigController.class);

    private final ModelConfigService modelConfigService;

    public ModelConfigController(ModelConfigService modelConfigService) {
        this.modelConfigService = modelConfigService;
    }

    /**
     * GET /api/model/config - 获取当前模型配置
     */
    @GetMapping("/config")
    public ResponseEntity<ModelConfig> getConfig() {
        ModelConfig config = modelConfigService.getConfig();
        return ResponseEntity.ok(config);
    }

    /**
     * PUT /api/model/config - 更新模型配置
     */
    @PutMapping("/config")
    public ResponseEntity<ModelConfig> updateConfig(@RequestBody ModelConfig newConfig) {
        logger.info("Updating model config via API: provider={}, model={}",
            newConfig.provider(), newConfig.modelName());
        ModelConfig updated = modelConfigService.updateConfig(newConfig);
        return ResponseEntity.ok(updated);
    }

    /**
     * POST /api/model/config/test - 测试模型连接
     */
    @GetMapping("/config/test")
    public ResponseEntity<ConnectionTestResult> testConnection() {
        boolean success = modelConfigService.testConnection();
        ModelConfig config = modelConfigService.getConfig();
        return ResponseEntity.ok(new ConnectionTestResult(
            success,
            success ? "连接成功" : "连接失败",
            config.provider(),
            config.modelName()
        ));
    }

    public record ConnectionTestResult(
        boolean success,
        String message,
        String provider,
        String model
    ) {}
}
