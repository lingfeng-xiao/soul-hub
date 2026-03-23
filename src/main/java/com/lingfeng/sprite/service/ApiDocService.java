package com.lingfeng.sprite.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * S11-7: API文档自动化服务
 *
 * 自动生成和管理API文档：
 * - 从服务方法自动提取API端点信息
 * - 生成OpenAPI格式的文档数据
 * - 支持API变更追踪
 * - 提供文档搜索和过滤
 */
public class ApiDocService {

    private static final Logger logger = LoggerFactory.getLogger(ApiDocService.class);

    private final Map<String, ApiEndpointDoc> endpoints;
    private final Map<String, ApiServiceDoc> services;
    private final List<ApiChangeRecord> changeHistory;
    private Instant lastGenerated;

    /**
     * API端点文档
     */
    public record ApiEndpointDoc(
        String path,
        String method,
        String summary,
        String description,
        List<String> tags,
        Map<String, ApiParameterDoc> parameters,
        ApiRequestBodyDoc requestBody,
        ApiResponseDoc response,
        List<String> produces,
        List<String> consumes,
        boolean deprecated,
        Instant addedAt,
        Instant lastModified
    ) {}

    /**
     * API服务文档
     */
    public record ApiServiceDoc(
        String name,
        String description,
        List<String> endpoints,
        Instant addedAt
    ) {}

    /**
     * API参数文档
     */
    public record ApiParameterDoc(
        String name,
        String location,
        String type,
        boolean required,
        String description,
        String defaultValue
    ) {}

    /**
     * API请求体文档
     */
    public record ApiRequestBodyDoc(
        String contentType,
        String description,
        List<ApiSchemaDoc> schema
    ) {}

    /**
     * API响应文档
     */
    public record ApiResponseDoc(
        int statusCode,
        String description,
        ApiSchemaDoc schema
    ) {}

    /**
     * API模式文档
     */
    public record ApiSchemaDoc(
        String name,
        String type,
        String format,
        String description,
        boolean required
    ) {}

    /**
     * API变更记录
     */
    public record ApiChangeRecord(
        Instant timestamp,
        String endpointPath,
        String changeType,
        String description
    ) {}

    /**
     * API文档统计
     */
    public record ApiDocStats(
        int totalEndpoints,
        int totalServices,
        int deprecatedEndpoints,
        Instant lastGenerated,
        Map<String, Integer> endpointsByTag
    ) {}

    /**
     * 完整API文档
     */
    public record ApiDocumentation(
        String title,
        String version,
        String description,
        Instant generatedAt,
        List<ApiServiceDoc> services,
        List<ApiEndpointDoc> endpoints,
        List<ApiChangeRecord> changeHistory
    ) {}

    public ApiDocService() {
        this.endpoints = new ConcurrentHashMap<>();
        this.services = new ConcurrentHashMap<>();
        this.changeHistory = new ArrayList<>();
        this.lastGenerated = Instant.now();
    }

    // ==================== 端点注册 ====================

    /**
     * 注册API端点
     */
    public void registerEndpoint(ApiEndpointDoc endpoint) {
        String key = endpoint.method().toUpperCase() + " " + endpoint.path();
        endpoints.put(key, endpoint);
        lastGenerated = Instant.now();

        changeHistory.add(new ApiChangeRecord(
            Instant.now(),
            endpoint.path(),
            "ADDED",
            "Endpoint registered: " + endpoint.summary()
        ));

        logger.info("Registered API endpoint: {} {}", endpoint.method(), endpoint.path());
    }

    /**
     * 注册API端点（简化版本）
     */
    public void registerEndpoint(String method, String path, String summary, String description) {
        registerEndpoint(new ApiEndpointDoc(
            path,
            method.toUpperCase(),
            summary,
            description,
            new ArrayList<>(),
            new ConcurrentHashMap<>(),
            null,
            null,
            List.of("application/json"),
            List.of("application/json"),
            false,
            Instant.now(),
            Instant.now()
        ));
    }

    /**
     * 注册带标签的API端点
     */
    public void registerEndpoint(String method, String path, String summary, String description, List<String> tags) {
        registerEndpoint(new ApiEndpointDoc(
            path,
            method.toUpperCase(),
            summary,
            description,
            new ArrayList<>(tags),
            new ConcurrentHashMap<>(),
            null,
            null,
            List.of("application/json"),
            List.of("application/json"),
            false,
            Instant.now(),
            Instant.now()
        ));
    }

    /**
     * 批量注册API端点
     */
    public void registerEndpoints(List<ApiEndpointDoc> endpoints) {
        endpoints.forEach(this::registerEndpoint);
    }

    // ==================== 服务注册 ====================

    /**
     * 注册API服务
     */
    public void registerService(ApiServiceDoc service) {
        services.put(service.name(), service);
        logger.info("Registered API service: {}", service.name());
    }

    /**
     * 注册API服务（简化版本）
     */
    public void registerService(String name, String description) {
        registerService(new ApiServiceDoc(
            name,
            description,
            new ArrayList<>(),
            Instant.now()
        ));
    }

    // ==================== 获取操作 ====================

    /**
     * 获取端点文档
     */
    public ApiEndpointDoc getEndpoint(String method, String path) {
        String key = method.toUpperCase() + " " + path;
        return endpoints.get(key);
    }

    /**
     * 获取所有端点
     */
    public List<ApiEndpointDoc> getAllEndpoints() {
        return new ArrayList<>(endpoints.values());
    }

    /**
     * 按标签获取端点
     */
    public List<ApiEndpointDoc> getEndpointsByTag(String tag) {
        return endpoints.values().stream()
            .filter(e -> e.tags().contains(tag))
            .collect(Collectors.toList());
    }

    /**
     * 按前缀获取端点
     */
    public List<ApiEndpointDoc> getEndpointsByPathPrefix(String prefix) {
        return endpoints.values().stream()
            .filter(e -> e.path().startsWith(prefix))
            .collect(Collectors.toList());
    }

    /**
     * 获取服务文档
     */
    public ApiServiceDoc getService(String name) {
        return services.get(name);
    }

    /**
     * 获取所有服务
     */
    public List<ApiServiceDoc> getAllServices() {
        return new ArrayList<>(services.values());
    }

    /**
     * 获取变更历史
     */
    public List<ApiChangeRecord> getChangeHistory(int limit) {
        return changeHistory.stream()
            .sorted(Comparator.comparing(ApiChangeRecord::timestamp).reversed())
            .limit(limit)
            .collect(Collectors.toList());
    }

    /**
     * 获取简化文档（仅基本信息）
     */
    public ApiDocumentation getSimpleDocumentation() {
        return new ApiDocumentation(
            "Sprite API Documentation",
            "1.0.0",
            "Sprite Digital Being API Reference",
            Instant.now(),
            getAllServices(),
            getAllEndpoints(),
            getChangeHistory(50)
        );
    }

    // ==================== 生成文档 ====================

    /**
     * 生成完整OpenAPI格式文档
     */
    public Map<String, Object> generateOpenApiDoc() {
        Map<String, Object> doc = new HashMap<>();

        doc.put("openapi", "3.0.3");
        doc.put("info", Map.of(
            "title", "Sprite Digital Being API",
            "description", "API documentation for Sprite Digital Being system",
            "version", "1.0.0"
        ));
        doc.put("paths", generatePaths());
        doc.put("components", generateComponents());

        return doc;
    }

    private Map<String, Object> generatePaths() {
        Map<String, Object> paths = new HashMap<>();

        endpoints.values().forEach(endpoint -> {
            Map<String, Object> pathItem = new HashMap<>();

            Map<String, Object> operation = new HashMap<>();
            operation.put("summary", endpoint.summary());
            operation.put("description", endpoint.description());
            operation.put("tags", endpoint.tags());
            operation.put("deprecated", endpoint.deprecated());

            if (!endpoint.parameters().isEmpty()) {
                operation.put("parameters", generateParameters(endpoint.parameters()));
            }

            if (endpoint.requestBody() != null) {
                operation.put("requestBody", generateRequestBody(endpoint.requestBody()));
            }

            if (endpoint.response() != null) {
                operation.put("responses", generateResponses(endpoint.response()));
            }

            pathItem.put(endpoint.method().toLowerCase(), operation);
            paths.put(endpoint.path(), pathItem);
        });

        return paths;
    }

    private List<Map<String, Object>> generateParameters(Map<String, ApiParameterDoc> params) {
        return params.values().stream()
            .map(p -> {
                Map<String, Object> param = new HashMap<>();
                param.put("name", p.name());
                param.put("in", p.location());
                param.put("required", p.required());
                param.put("description", p.description());
                param.put("schema", Map.of("type", p.type()));
                return param;
            })
            .collect(Collectors.toList());
    }

    private Map<String, Object> generateRequestBody(ApiRequestBodyDoc body) {
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("description", body.description());
        requestBody.put("required", true);
        requestBody.put("content", Map.of(
            body.contentType(), Map.of(
                "schema", generateSchema(body.schema())
            )
        ));
        return requestBody;
    }

    private Map<String, Object> generateResponses(ApiResponseDoc response) {
        Map<String, Object> responses = new HashMap<>();
        Map<String, Object> responseObj = new HashMap<>();
        responseObj.put("description", response.description());
        if (response.schema() != null) {
            responseObj.put("content", Map.of(
                "application/json", Map.of("schema", generateSchema(List.of(response.schema())))
            ));
        }
        responses.put(String.valueOf(response.statusCode()), responseObj);
        return responses;
    }

    private Map<String, Object> generateSchema(List<ApiSchemaDoc> schemaList) {
        if (schemaList == null || schemaList.isEmpty()) {
            return Map.of("type", "object");
        }

        Map<String, Object> properties = new HashMap<>();
        List<String> required = new ArrayList<>();

        schemaList.forEach(s -> {
            Map<String, Object> prop = new HashMap<>();
            prop.put("type", s.type());
            if (s.format() != null) prop.put("format", s.format());
            if (s.description() != null) prop.put("description", s.description());
            properties.put(s.name(), prop);
            if (s.required()) required.add(s.name());
        });

        return Map.of(
            "type", "object",
            "properties", properties,
            "required", required
        );
    }

    private Map<String, Object> generateComponents() {
        Map<String, Object> components = new HashMap<>();
        components.put("schemas", new HashMap<>());
        return components;
    }

    // ==================== 搜索和过滤 ====================

    /**
     * 搜索端点
     */
    public List<ApiEndpointDoc> searchEndpoints(String query) {
        String lowerQuery = query.toLowerCase();
        return endpoints.values().stream()
            .filter(e ->
                e.path().toLowerCase().contains(lowerQuery) ||
                e.summary().toLowerCase().contains(lowerQuery) ||
                e.description().toLowerCase().contains(lowerQuery) ||
                e.tags().stream().anyMatch(t -> t.toLowerCase().contains(lowerQuery))
            )
            .collect(Collectors.toList());
    }

    /**
     * 获取已弃用的端点
     */
    public List<ApiEndpointDoc> getDeprecatedEndpoints() {
        return endpoints.values().stream()
            .filter(ApiEndpointDoc::deprecated)
            .collect(Collectors.toList());
    }

    // ==================== 统计 ====================

    /**
     * 获取文档统计
     */
    public ApiDocStats getStats() {
        Map<String, Integer> endpointsByTag = new HashMap<>();
        endpoints.values().forEach(e ->
            e.tags().forEach(t ->
                endpointsByTag.merge(t, 1, Integer::sum)
            )
        );

        return new ApiDocStats(
            endpoints.size(),
            services.size(),
            (int) endpoints.values().stream().filter(ApiEndpointDoc::deprecated).count(),
            lastGenerated,
            endpointsByTag
        );
    }

    // ==================== 导出 ====================

    /**
     * 导出为JSON格式
     */
    public String exportToJson() {
        try {
            var objectMapper = new com.fasterxml.jackson.databind.ObjectMapper();
            objectMapper.findAndRegisterModules();
            return objectMapper.writerWithDefaultPrettyPrinter()
                .writeValueAsString(getSimpleDocumentation());
        } catch (Exception e) {
            logger.error("Failed to export documentation to JSON", e);
            return "{}";
        }
    }

    /**
     * 导出变更历史
     */
    public String exportChangeHistory() {
        try {
            var objectMapper = new com.fasterxml.jackson.databind.ObjectMapper();
            objectMapper.findAndRegisterModules();
            return objectMapper.writerWithDefaultPrettyPrinter()
                .writeValueAsString(getChangeHistory(100));
        } catch (Exception e) {
            logger.error("Failed to export change history", e);
            return "[]";
        }
    }
}
