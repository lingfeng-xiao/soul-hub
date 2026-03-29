package com.lingfeng.sprite.controller.dto;

import java.util.Locale;
import java.util.Map;

public record LifeCommandRequest(
        String type,
        String content,
        Map<String, Object> context,
        String source
) {
    public LifeCommandRequest {
        type = type == null ? "ASK" : type.trim().toUpperCase(Locale.ROOT);
        content = content == null ? "" : content.trim();
        context = context == null ? Map.of() : Map.copyOf(context);
        source = source == null || source.isBlank() ? "ui" : source.trim();
    }
}
