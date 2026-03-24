package com.lingfeng.sprite.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

/**
 * S32-1 & S32-4: OpenAPI Documentation and Third-party Integration Service
 */
@Service
public class OpenApiDocumentation {

    private static final Logger logger = LoggerFactory.getLogger(OpenApiDocumentation.class);

    public enum SdkLanguage {
        JAVA("Java", "java"),
        PYTHON("Python", "python"),
        JAVASCRIPT("JavaScript", "javascript"),
        TYPESCRIPT("TypeScript", "typescript");

        private final String displayName;
        private final String fileExtension;

        SdkLanguage(String displayName, String fileExtension) {
            this.displayName = displayName;
            this.fileExtension = fileExtension;
        }

        public String getDisplayName() { return displayName; }
        public String getFileExtension() { return fileExtension; }
    }

    public record ApiEndpoint(
        String path,
        String method,
        String summary,
        String description,
        List<ApiParameter> parameters,
        ApiRequestBody requestBody,
        Map<String, ApiResponse> responses,
        List<String> security,
        List<String> tags
    ) {}

    public record ApiParameter(
        String name,
        String location,
        String type,
        boolean required,
        String description,
        String example
    ) {}

    public record ApiRequestBody(
        String contentType,
        String description,
        Map<String, String> schema,
        String example
    ) {}

    public record ApiResponse(
        int statusCode,
        String description,
        String contentType,
        Map<String, String> schema,
        String example
    ) {}

    public record ApiSchemaField(
        String name,
        String type,
        String format,
        String description,
        boolean required,
        String example
    ) {}

    private final ObjectMapper objectMapper;

    public OpenApiDocumentation() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
        this.objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
    }

    /**
     * S32-1: Generate OpenAPI 3.0 specification
     */
    public String generateOpenApiSpec() {
        Map<String, Object> spec = new LinkedHashMap<>();
        spec.put("openapi", "3.0.3");
        spec.put("info", createInfo());
        spec.put("servers", createServers());
        spec.put("paths", createPaths());
        spec.put("components", createComponents());

        try {
            return objectMapper.writeValueAsString(spec);
        } catch (JsonProcessingException e) {
            logger.error("Failed to generate OpenAPI spec", e);
            return "{}";
        }
    }

    private Map<String, Object> createInfo() {
        Map<String, Object> info = new LinkedHashMap<>();
        info.put("title", "Sprite Digital Being API");
        info.put("description", "API for Sprite digital being interaction and control");
        info.put("version", "1.0.0");
        info.put("contact", Map.of("name", "Sprite Support", "email", "support@sprite.example.com"));
        return info;
    }

    private List<Map<String, String>> createServers() {
        List<Map<String, String>> servers = new ArrayList<>();
        servers.add(Map.of("url", "http://localhost:8080", "description", "Local development server"));
        return servers;
    }

    private Map<String, Object> createPaths() {
        Map<String, Object> paths = new LinkedHashMap<>();

        // Sprite state endpoint
        Map<String, Object> statePath = new LinkedHashMap<>();
        Map<String, Object> getState = new LinkedHashMap<>();
        getState.put("summary", "Get current Sprite state");
        getState.put("description", "Returns the current state of the Sprite including mood, active goals, and cognition stats");
        getState.put("responses", Map.of(
            "200", Map.of("description", "Successful response", "content", Map.of("application/json", Map.of("schema", Map.of("$ref", "#/components/schemas/State"))))
        ));
        statePath.put("get", getState);
        paths.put("/api/sprite/state", statePath);

        // Cognition cycle endpoint
        Map<String, Object> cyclePath = new LinkedHashMap<>();
        Map<String, Object> postCycle = new LinkedHashMap<>();
        postCycle.put("summary", "Trigger cognition cycle");
        postCycle.put("description", "Manually triggers a cognition cycle");
        postCycle.put("responses", Map.of("200", Map.of("description", "Cycle triggered")));
        cyclePath.put("post", postCycle);
        paths.put("/api/sprite/cycle", cyclePath);

        // Memory endpoints
        Map<String, Object> memoryPath = new LinkedHashMap<>();
        Map<String, Object> getMemory = new LinkedHashMap<>();
        getMemory.put("summary", "Get memory status");
        getMemory.put("responses", Map.of("200", Map.of("description", "Memory status")));
        memoryPath.put("get", getMemory);
        paths.put("/api/sprite/memory", memoryPath);

        return paths;
    }

    private Map<String, Object> createComponents() {
        Map<String, Object> components = new LinkedHashMap<>();
        components.put("schemas", createSchemas());
        components.put("securitySchemes", createSecuritySchemes());
        return components;
    }

    private Map<String, Object> createSchemas() {
        Map<String, Object> schemas = new LinkedHashMap<>();

        Map<String, Object> stateSchema = new LinkedHashMap<>();
        stateSchema.put("type", "object");
        stateSchema.put("properties", Map.of(
            "state", Map.of("type", "string", "description", "Current state"),
            "mood", Map.of("type", "string", "description", "Current mood")
        ));
        schemas.put("State", stateSchema);

        return schemas;
    }

    private Map<String, Object> createSecuritySchemes() {
        Map<String, Object> schemes = new LinkedHashMap<>();

        Map<String, Object> apiKeyScheme = new LinkedHashMap<>();
        apiKeyScheme.put("type", "apiKey");
        apiKeyScheme.put("in", "header");
        apiKeyScheme.put("name", "X-API-Key");
        apiKeyScheme.put("description", "API key authentication");
        schemes.put("ApiKeyAuth", apiKeyScheme);

        Map<String, Object> oauth2Scheme = new LinkedHashMap<>();
        oauth2Scheme.put("type", "oauth2");
        oauth2Scheme.put("flows", Map.of(
            "authorizationCode", Map.of(
                "authorizationUrl", "https://auth.example.com/oauth/authorize",
                "tokenUrl", "https://auth.example.com/oauth/token",
                "scopes", Map.of("read", "Read access", "write", "Write access")
            )
        ));
        schemes.put("OAuth2", oauth2Scheme);

        return schemes;
    }

    /**
     * S32-1: Get list of API endpoints
     */
    public List<ApiEndpoint> getEndpoints() {
        List<ApiEndpoint> endpoints = new ArrayList<>();

        endpoints.add(new ApiEndpoint(
            "/api/sprite/state", "GET",
            "Get current Sprite state",
            "Returns the current state of the Sprite",
            List.of(),
            null,
            Map.of("200", new ApiResponse(200, "Successful response", "application/json", Map.of("state", "string"), "{}")),
            List.of("ApiKeyAuth"),
            List.of("Sprite")
        ));

        endpoints.add(new ApiEndpoint(
            "/api/sprite/cycle", "POST",
            "Trigger cognition cycle",
            "Manually triggers a cognition cycle",
            List.of(),
            null,
            Map.of("200", new ApiResponse(200, "Cycle triggered", "application/json", Map.of("status", "string"), "{}")),
            List.of("ApiKeyAuth"),
            List.of("Sprite")
        ));

        endpoints.add(new ApiEndpoint(
            "/api/sprite/memory", "GET",
            "Get memory status",
            "Returns the current memory system status",
            List.of(),
            null,
            Map.of("200", new ApiResponse(200, "Memory status", "application/json", Map.of("status", "string"), "{}")),
            List.of("ApiKeyAuth"),
            List.of("Memory")
        ));

        endpoints.add(new ApiEndpoint(
            "/api/sprite/memory/visualization", "GET",
            "Get memory visualization",
            "Returns memory data for visualization",
            List.of(),
            null,
            Map.of("200", new ApiResponse(200, "Visualization data", "application/json", Map.of("data", "object"), "{}")),
            List.of("ApiKeyAuth"),
            List.of("Memory")
        ));

        return endpoints;
    }

    /**
     * S32-4: Generate SDK client code
     */
    public String generateSdkClient(SdkLanguage language) {
        return switch (language) {
            case JAVA -> generateJavaSdk();
            case PYTHON -> generatePythonSdk();
            case JAVASCRIPT -> generateJavaScriptSdk();
            case TYPESCRIPT -> generateTypeScriptSdk();
        };
    }

    private String generateJavaSdk() {
        StringBuilder sb = new StringBuilder();
        sb.append("package com.sprite.sdk;\n\n");
        sb.append("import java.net.http.*;\n");
        sb.append("import java.net.URI;\n");
        sb.append("import java.time.Duration;\n\n");
        sb.append("public class SpriteClient {\n");
        sb.append("    private final String baseUrl;\n");
        sb.append("    private final String apiKey;\n");
        sb.append("    private final HttpClient httpClient;\n\n");
        sb.append("    public SpriteClient(String apiKey) {\n");
        sb.append("        this(\"http://localhost:8080\", apiKey);\n");
        sb.append("    }\n\n");
        sb.append("    public SpriteClient(String baseUrl, String apiKey) {\n");
        sb.append("        this.baseUrl = baseUrl;\n");
        sb.append("        this.apiKey = apiKey;\n");
        sb.append("        this.httpClient = HttpClient.newHttpClient();\n");
        sb.append("    }\n\n");
        sb.append("    public String getState() throws Exception {\n");
        sb.append("        return get(\"/api/sprite/state\");\n");
        sb.append("    }\n\n");
        sb.append("    public String triggerCognitionCycle() throws Exception {\n");
        sb.append("        return post(\"/api/sprite/cycle\", \"{}\");\n");
        sb.append("    }\n\n");
        sb.append("    public String getMemoryStatus() throws Exception {\n");
        sb.append("        return get(\"/api/sprite/memory\");\n");
        sb.append("    }\n\n");
        sb.append("    private String get(String path) throws Exception {\n");
        sb.append("        HttpRequest request = HttpRequest.newBuilder()\n");
        sb.append("            .uri(URI.create(baseUrl + path))\n");
        sb.append("            .header(\"X-API-Key\", apiKey)\n");
        sb.append("            .GET()\n");
        sb.append("            .build();\n");
        sb.append("        return httpClient.send(request, HttpResponse.BodyHandlers.ofString()).body();\n");
        sb.append("    }\n\n");
        sb.append("    private String post(String path, String body) throws Exception {\n");
        sb.append("        HttpRequest request = HttpRequest.newBuilder()\n");
        sb.append("            .uri(URI.create(baseUrl + path))\n");
        sb.append("            .header(\"X-API-Key\", apiKey)\n");
        sb.append("            .header(\"Content-Type\", \"application/json\")\n");
        sb.append("            .POST(HttpRequest.BodyPublishers.ofString(body))\n");
        sb.append("            .build();\n");
        sb.append("        return httpClient.send(request, HttpResponse.BodyHandlers.ofString()).body();\n");
        sb.append("    }\n");
        sb.append("}\n");
        return sb.toString();
    }

    private String generatePythonSdk() {
        StringBuilder sb = new StringBuilder();
        sb.append("#!/usr/bin/env python3\n");
        sb.append("# Sprite Python SDK Client\n");
        sb.append("# Generated: ").append(Instant.now()).append("\n\n");
        sb.append("import requests\n");
        sb.append("from typing import Optional, Dict, Any\n\n\n");
        sb.append("class SpriteClient:\n");
        sb.append("    BASE_URL = \"http://localhost:8080\"\n\n");
        sb.append("    def __init__(self, api_key: str, base_url: Optional[str] = None):\n");
        sb.append("        self.api_key = api_key\n");
        sb.append("        self.base_url = base_url or self.BASE_URL\n");
        sb.append("        self.session = requests.Session()\n");
        sb.append("        self.session.headers.update({\n");
        sb.append("            \"X-API-Key\": api_key,\n");
        sb.append("            \"Content-Type\": \"application/json\"\n");
        sb.append("        })\n\n");
        sb.append("    def _request(self, method: str, path: str, **kwargs) -> Dict[str, Any]:\n");
        sb.append("        url = f\"{self.base_url}{path}\"\n");
        sb.append("        response = self.session.request(method, url, **kwargs)\n");
        sb.append("        response.raise_for_status()\n");
        sb.append("        return response.json()\n\n");
        sb.append("    def get_state(self) -> Dict[str, Any]:\n");
        sb.append("        return self._request(\"GET\", \"/api/sprite/state\")\n\n");
        sb.append("    def trigger_cognition_cycle(self) -> Dict[str, Any]:\n");
        sb.append("        return self._request(\"POST\", \"/api/sprite/cycle\")\n\n");
        sb.append("    def get_memory_status(self) -> Dict[str, Any]:\n");
        sb.append("        return self._request(\"GET\", \"/api/sprite/memory\")\n\n");
        return sb.toString();
    }

    private String generateJavaScriptSdk() {
        StringBuilder sb = new StringBuilder();
        sb.append("// Sprite JavaScript SDK Client\n");
        sb.append("// Generated: ").append(Instant.now()).append("\n\n");
        sb.append("class SpriteClient {\n");
        sb.append("    constructor(apiKey, baseUrl = 'http://localhost:8080') {\n");
        sb.append("        this.apiKey = apiKey;\n");
        sb.append("        this.baseUrl = baseUrl;\n");
        sb.append("    }\n\n");
        sb.append("    async _request(method, path, body = null) {\n");
        sb.append("        const options = {\n");
        sb.append("            method,\n");
        sb.append("            headers: {\n");
        sb.append("                'X-API-Key': this.apiKey,\n");
        sb.append("                'Content-Type': 'application/json'\n");
        sb.append("            }\n");
        sb.append("        };\n");
        sb.append("        if (body) {\n");
        sb.append("            options.body = JSON.stringify(body);\n");
        sb.append("        }\n");
        sb.append("        const response = await fetch(`${this.baseUrl}${path}`, options);\n");
        sb.append("        if (!response.ok) {\n");
        sb.append("            throw new Error(`HTTP error! status: ${response.status}`);\n");
        sb.append("        }\n");
        sb.append("        return response.json();\n");
        sb.append("    }\n\n");
        sb.append("    async getState() {\n");
        sb.append("        return this._request('GET', '/api/sprite/state');\n");
        sb.append("    }\n\n");
        sb.append("    async triggerCognitionCycle() {\n");
        sb.append("        return this._request('POST', '/api/sprite/cycle');\n");
        sb.append("    }\n\n");
        sb.append("    async getMemoryStatus() {\n");
        sb.append("        return this._request('GET', '/api/sprite/memory');\n");
        sb.append("    }\n");
        sb.append("}\n\n");
        sb.append("module.exports = SpriteClient;\n");
        return sb.toString();
    }

    private String generateTypeScriptSdk() {
        StringBuilder sb = new StringBuilder();
        sb.append("// Sprite TypeScript SDK Client\n");
        sb.append("// Generated: ").append(Instant.now()).append("\n\n");
        sb.append("interface SpriteState {\n");
        sb.append("    state: string;\n");
        sb.append("    mood?: string;\n");
        sb.append("}\n\n");
        sb.append("class SpriteClient {\n");
        sb.append("    private baseUrl: string;\n");
        sb.append("    private apiKey: string;\n\n");
        sb.append("    constructor(apiKey: string, baseUrl: string = 'http://localhost:8080') {\n");
        sb.append("        this.apiKey = apiKey;\n");
        sb.append("        this.baseUrl = baseUrl;\n");
        sb.append("    }\n\n");
        sb.append("    private async request<T>(method: string, path: string, body?: object): Promise<T> {\n");
        sb.append("        const options: RequestInit = {\n");
        sb.append("            method,\n");
        sb.append("            headers: {\n");
        sb.append("                'X-API-Key': this.apiKey,\n");
        sb.append("                'Content-Type': 'application/json'\n");
        sb.append("            }\n");
        sb.append("        };\n");
        sb.append("        if (body) {\n");
        sb.append("            options.body = JSON.stringify(body);\n");
        sb.append("        }\n");
        sb.append("        const response = await fetch(`${this.baseUrl}${path}`, options);\n");
        sb.append("        if (!response.ok) {\n");
        sb.append("            throw new Error(`HTTP error! status: ${response.status}`);\n");
        sb.append("        }\n");
        sb.append("        return response.json();\n");
        sb.append("    }\n\n");
        sb.append("    async getState(): Promise<SpriteState> {\n");
        sb.append("        return this.request<SpriteState>('GET', '/api/sprite/state');\n");
        sb.append("    }\n\n");
        sb.append("    async triggerCognitionCycle(): Promise<object> {\n");
        sb.append("        return this.request<object>('POST', '/api/sprite/cycle');\n");
        sb.append("    }\n\n");
        sb.append("    async getMemoryStatus(): Promise<object> {\n");
        sb.append("        return this.request<object>('GET', '/api/sprite/memory');\n");
        sb.append("    }\n");
        sb.append("}\n\n");
        sb.append("export default SpriteClient;\n");
        return sb.toString();
    }

    /**
     * S32-4: Get integration guide
     */
    public String getIntegrationGuide() {
        StringBuilder sb = new StringBuilder();
        sb.append("# Sprite API Integration Guide\n\n");
        sb.append("## Authentication\n\n");
        sb.append("All API requests require an API key passed in the `X-API-Key` header:\n\n");
        sb.append("```bash\n");
        sb.append("curl -H \"X-API-Key: your-api-key\" http://localhost:8080/api/sprite/state\n");
        sb.append("```\n\n");
        sb.append("## Rate Limiting\n\n");
        sb.append("- Default: 100 requests per minute\n");
        sb.append("- Authenticated: 1000 requests per minute\n\n");
        sb.append("## Error Handling\n\n");
        sb.append("| Status Code | Meaning |\n");
        sb.append("|-------------|---------|\n");
        sb.append("| 400 | Bad Request - Invalid parameters |\n");
        sb.append("| 401 | Unauthorized - Invalid or missing API key |\n");
        sb.append("| 429 | Too Many Requests - Rate limit exceeded |\n");
        sb.append("| 500 | Internal Server Error |\n\n");
        sb.append("## SDK Usage\n\n");
        sb.append("### Python\n\n");
        sb.append("```python\n");
        sb.append("from sprite_sdk import SpriteClient\n\n");
        sb.append("client = SpriteClient('your-api-key')\n");
        sb.append("state = client.get_state()\n");
        sb.append("```\n\n");
        sb.append("### JavaScript\n\n");
        sb.append("```javascript\n");
        sb.append("const { SpriteClient } = require('./sprite-sdk');\n\n");
        sb.append("const client = new SpriteClient('your-api-key');\n");
        sb.append("const state = await client.getState();\n");
        sb.append("```\n");
        return sb.toString();
    }

    /**
     * Validate OpenAPI specification
     */
    public boolean validateOpenApiSpec(String spec) {
        try {
            Map<?, ?> parsed = objectMapper.readValue(spec, Map.class);
            return parsed.containsKey("openapi") &&
                   parsed.containsKey("info") &&
                   parsed.containsKey("paths");
        } catch (Exception e) {
            logger.error("OpenAPI spec validation failed", e);
            return false;
        }
    }

    /**
     * Export as Postman collection
     */
    public String exportAsPostmanCollection() {
        Map<String, Object> collection = new LinkedHashMap<>();
        collection.put("info", Map.of(
            "name", "Sprite API",
            "schema", "https://schema.getpostman.com/json/collection/v2.1.0/collection.json"
        ));
        collection.put("item", createPostmanItems());
        try {
            return objectMapper.writeValueAsString(collection);
        } catch (JsonProcessingException e) {
            logger.error("Failed to export Postman collection", e);
            return "{}";
        }
    }

    private List<Map<String, Object>> createPostmanItems() {
        List<Map<String, Object>> items = new ArrayList<>();
        for (ApiEndpoint endpoint : getEndpoints()) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("name", endpoint.summary());
            item.put("request", Map.of(
                "method", endpoint.method(),
                "header", List.of(Map.of("key", "X-API-Key", "value", "{{api_key}}")),
                "url", Map.of("raw", "{{base_url}}" + endpoint.path())
            ));
            items.add(item);
        }
        return items;
    }

    /**
     * Get endpoints by tag
     */
    public List<ApiEndpoint> getEndpointsByTag(String tag) {
        return getEndpoints().stream()
            .filter(e -> e.tags() != null && e.tags().contains(tag))
            .collect(Collectors.toList());
    }

    /**
     * Get available SDK languages
     */
    public List<SdkLanguage> getAvailableLanguages() {
        return List.of(SdkLanguage.values());
    }
}
