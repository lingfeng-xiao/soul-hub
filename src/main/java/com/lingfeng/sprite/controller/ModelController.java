package com.lingfeng.sprite.controller;

import com.lingfeng.sprite.controller.dto.ModelConfigDto;
import com.lingfeng.sprite.controller.dto.ModelConnectionTestResult;
import com.lingfeng.sprite.life.ModelConfigService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/model")
public class ModelController {

    private final ModelConfigService modelConfigService;

    public ModelController(ModelConfigService modelConfigService) {
        this.modelConfigService = modelConfigService;
    }

    @GetMapping("/config")
    public ResponseEntity<ModelConfigDto> getConfig() {
        return ResponseEntity.ok(modelConfigService.getConfig());
    }

    @PutMapping("/config")
    public ResponseEntity<ModelConfigDto> updateConfig(@RequestBody ModelConfigDto config) {
        return ResponseEntity.ok(modelConfigService.update(config));
    }

    @PostMapping("/test")
    public ResponseEntity<ModelConnectionTestResult> testConnection() {
        return ResponseEntity.ok(modelConfigService.testConnection());
    }
}
