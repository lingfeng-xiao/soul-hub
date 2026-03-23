package com.lingfeng.sprite.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ApiDocService 单元测试
 */
class ApiDocServiceTest {

    private ApiDocService docService;

    @BeforeEach
    void setUp() {
        docService = new ApiDocService();
    }

    @Test
    void testRegisterEndpointSimplified() {
        docService.registerEndpoint("GET", "/api/test", "Test endpoint", "Test description");

        ApiDocService.ApiEndpointDoc endpoint = docService.getEndpoint("GET", "/api/test");
        assertNotNull(endpoint);
        assertEquals("/api/test", endpoint.path());
        assertEquals("GET", endpoint.method());
        assertEquals("Test endpoint", endpoint.summary());
        assertEquals("Test description", endpoint.description());
        assertFalse(endpoint.deprecated());
        assertNotNull(endpoint.addedAt());
        assertNotNull(endpoint.lastModified());
    }

    @Test
    void testRegisterEndpointWithTags() {
        List<String> tags = List.of("User", "Management");
        docService.registerEndpoint("POST", "/api/users", "Create user", "Create a new user", tags);

        ApiDocService.ApiEndpointDoc endpoint = docService.getEndpoint("POST", "/api/users");
        assertNotNull(endpoint);
        assertEquals(2, endpoint.tags().size());
        assertTrue(endpoint.tags().contains("User"));
        assertTrue(endpoint.tags().contains("Management"));
    }

    @Test
    void testRegisterEndpointFull() {
        ApiDocService.ApiEndpointDoc endpoint = new ApiDocService.ApiEndpointDoc(
            "/api/full",
            "PUT",
            "Full endpoint",
            "Full description",
            List.of("Test"),
            Map.of("param1", new ApiDocService.ApiParameterDoc(
                "id", "path", "string", true, "The ID", null
            )),
            null,
            new ApiDocService.ApiResponseDoc(200, "Success", null),
            List.of("application/json"),
            List.of("application/json"),
            false,
            Instant.now(),
            Instant.now()
        );

        docService.registerEndpoint(endpoint);

        ApiDocService.ApiEndpointDoc retrieved = docService.getEndpoint("PUT", "/api/full");
        assertNotNull(retrieved);
        assertEquals("Full endpoint", retrieved.summary());
        assertEquals(1, retrieved.parameters().size());
        assertNotNull(retrieved.response());
    }

    @Test
    void testGetNonExistentEndpoint() {
        ApiDocService.ApiEndpointDoc endpoint = docService.getEndpoint("GET", "/non/existent");
        assertNull(endpoint);
    }

    @Test
    void testGetAllEndpoints() {
        docService.registerEndpoint("GET", "/api/1", "Endpoint 1", "Description 1");
        docService.registerEndpoint("POST", "/api/2", "Endpoint 2", "Description 2");
        docService.registerEndpoint("PUT", "/api/3", "Endpoint 3", "Description 3");

        List<ApiDocService.ApiEndpointDoc> all = docService.getAllEndpoints();
        assertEquals(3, all.size());
    }

    @Test
    void testGetEndpointsByTag() {
        docService.registerEndpoint("GET", "/api/users", "Get users", "Get all users", List.of("User"));
        docService.registerEndpoint("POST", "/api/users", "Create user", "Create user", List.of("User"));
        docService.registerEndpoint("GET", "/api/products", "Get products", "Get products", List.of("Product"));

        List<ApiDocService.ApiEndpointDoc> userEndpoints = docService.getEndpointsByTag("User");
        assertEquals(2, userEndpoints.size());

        List<ApiDocService.ApiEndpointDoc> productEndpoints = docService.getEndpointsByTag("Product");
        assertEquals(1, productEndpoints.size());
    }

    @Test
    void testGetEndpointsByPathPrefix() {
        docService.registerEndpoint("GET", "/api/v1/users", "Get users v1", "Description");
        docService.registerEndpoint("GET", "/api/v1/products", "Get products v1", "Description");
        docService.registerEndpoint("GET", "/api/v2/users", "Get users v2", "Description");

        List<ApiDocService.ApiEndpointDoc> v1Endpoints = docService.getEndpointsByPathPrefix("/api/v1");
        assertEquals(2, v1Endpoints.size());
    }

    @Test
    void testRegisterService() {
        docService.registerService("UserService", "User management service");

        ApiDocService.ApiServiceDoc service = docService.getService("UserService");
        assertNotNull(service);
        assertEquals("UserService", service.name());
        assertEquals("User management service", service.description());
        assertNotNull(service.addedAt());
    }

    @Test
    void testGetAllServices() {
        docService.registerService("Service1", "Description 1");
        docService.registerService("Service2", "Description 2");

        List<ApiDocService.ApiServiceDoc> services = docService.getAllServices();
        assertEquals(2, services.size());
    }

    @Test
    void testSearchEndpoints() {
        docService.registerEndpoint("GET", "/api/users/list", "List all users", "Get user list", List.of("User"));
        docService.registerEndpoint("GET", "/api/users/search", "Search users", "Search by name", List.of("User"));
        docService.registerEndpoint("GET", "/api/products", "List products", "Get product list", List.of("Product"));

        List<ApiDocService.ApiEndpointDoc> searchResults = docService.searchEndpoints("user");
        assertEquals(2, searchResults.size());

        List<ApiDocService.ApiEndpointDoc> searchByPath = docService.searchEndpoints("/api/users");
        assertEquals(2, searchByPath.size());
    }

    @Test
    void testGetChangeHistory() {
        docService.registerEndpoint("GET", "/api/change1", "Endpoint 1", "Description");
        docService.registerEndpoint("POST", "/api/change2", "Endpoint 2", "Description");

        List<ApiDocService.ApiChangeRecord> history = docService.getChangeHistory(10);
        assertEquals(2, history.size());
        assertTrue(history.get(0).timestamp().compareTo(history.get(1).timestamp()) >= 0);
    }

    @Test
    void testGetStats() {
        docService.registerEndpoint("GET", "/api/users", "Get users", "Get all users", List.of("User"));
        docService.registerEndpoint("POST", "/api/users", "Create user", "Create user", List.of("User"));
        docService.registerService("UserService", "User service");

        ApiDocService.ApiDocStats stats = docService.getStats();
        assertEquals(2, stats.totalEndpoints());
        assertEquals(1, stats.totalServices());
        assertEquals(0, stats.deprecatedEndpoints());
        assertNotNull(stats.lastGenerated());
        assertEquals(1, stats.endpointsByTag().size());
        assertEquals(2, stats.endpointsByTag().get("User"));
    }

    @Test
    void testGetSimpleDocumentation() {
        docService.registerEndpoint("GET", "/api/doc", "Doc endpoint", "Documentation");
        docService.registerService("DocService", "Documentation service");

        ApiDocService.ApiDocumentation doc = docService.getSimpleDocumentation();
        assertNotNull(doc);
        assertEquals("Sprite API Documentation", doc.title());
        assertEquals("1.0.0", doc.version());
        assertEquals(1, doc.endpoints().size());
        assertEquals(1, doc.services().size());
        assertNotNull(doc.generatedAt());
    }

    @Test
    void testGenerateOpenApiDoc() {
        docService.registerEndpoint("GET", "/api/openapi", "OpenAPI test", "Testing OpenAPI generation");

        Map<String, Object> openApiDoc = docService.generateOpenApiDoc();
        assertNotNull(openApiDoc);
        assertEquals("3.0.3", openApiDoc.get("openapi"));
        assertNotNull(openApiDoc.get("info"));
        assertNotNull(openApiDoc.get("paths"));
        assertNotNull(openApiDoc.get("components"));
    }

    @Test
    void testExportToJson() {
        docService.registerEndpoint("GET", "/api/export", "Export test", "Testing JSON export");

        String json = docService.exportToJson();
        assertNotNull(json);
        assertTrue(json.contains("Sprite API Documentation"));
        assertTrue(json.contains("/api/export"));
    }

    @Test
    void testExportChangeHistory() {
        docService.registerEndpoint("GET", "/api/history", "History test", "Testing history export");

        String history = docService.exportChangeHistory();
        assertNotNull(history);
        assertTrue(history.contains("ADDED"));
    }

    @Test
    void testDeprecatedEndpoints() {
        ApiDocService.ApiEndpointDoc deprecated = new ApiDocService.ApiEndpointDoc(
            "/api/deprecated",
            "DELETE",
            "Deprecated endpoint",
            "This endpoint is deprecated",
            List.of("Legacy"),
            new java.util.HashMap<>(),
            null,
            null,
            List.of("application/json"),
            List.of("application/json"),
            true,
            Instant.now(),
            Instant.now()
        );
        docService.registerEndpoint(deprecated);

        docService.registerEndpoint("GET", "/api/active", "Active endpoint", "This endpoint is active");

        List<ApiDocService.ApiEndpointDoc> deprecatedList = docService.getDeprecatedEndpoints();
        assertEquals(1, deprecatedList.size());
        assertEquals("/api/deprecated", deprecatedList.get(0).path());
    }

    @Test
    void testBatchRegister() {
        List<ApiDocService.ApiEndpointDoc> batch = List.of(
            new ApiDocService.ApiEndpointDoc(
                "/api/batch/1", "GET", "Batch 1", "First batch",
                List.of("Batch"), new java.util.HashMap<>(),
                null, null, List.of("application/json"), List.of("application/json"),
                false, Instant.now(), Instant.now()
            ),
            new ApiDocService.ApiEndpointDoc(
                "/api/batch/2", "POST", "Batch 2", "Second batch",
                List.of("Batch"), new java.util.HashMap<>(),
                null, null, List.of("application/json"), List.of("application/json"),
                false, Instant.now(), Instant.now()
            )
        );

        docService.registerEndpoints(batch);

        assertEquals(2, docService.getAllEndpoints().size());
    }

    @Test
    void testApiRecordTypes() {
        // Test ApiParameterDoc
        ApiDocService.ApiParameterDoc param = new ApiDocService.ApiParameterDoc(
            "id", "path", "string", true, "User ID", "123"
        );
        assertEquals("id", param.name());
        assertEquals("path", param.location());
        assertEquals("string", param.type());
        assertTrue(param.required());
        assertEquals("User ID", param.description());
        assertEquals("123", param.defaultValue());

        // Test ApiSchemaDoc
        ApiDocService.ApiSchemaDoc schema = new ApiDocService.ApiSchemaDoc(
            "User", "object", null, "User schema", true
        );
        assertEquals("User", schema.name());
        assertEquals("object", schema.type());
        assertTrue(schema.required());

        // Test ApiResponseDoc
        ApiDocService.ApiResponseDoc response = new ApiDocService.ApiResponseDoc(
            200, "Success", schema
        );
        assertEquals(200, response.statusCode());
        assertEquals("Success", response.description());
        assertNotNull(response.schema());
    }
}
