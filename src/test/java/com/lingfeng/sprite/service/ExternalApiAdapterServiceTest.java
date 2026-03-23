package com.lingfeng.sprite.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ExternalApiAdapterService 单元测试
 */
class ExternalApiAdapterServiceTest {

    private ExternalApiAdapterService apiService;

    @BeforeEach
    void setUp() {
        apiService = new ExternalApiAdapterService();
    }

    @Test
    void testRegisterEndpoint() {
        ExternalApiAdapterService.ApiEndpoint endpoint = new ExternalApiAdapterService.ApiEndpoint(
            "weather-api",
            "Weather API",
            ExternalApiAdapterService.ApiType.WEATHER,
            "https://api.weather.com",
            "api-key-123",
            Duration.ofSeconds(30),
            true
        );

        apiService.registerEndpoint(endpoint);

        ExternalApiAdapterService.ApiEndpoint retrieved = apiService.getEndpoint("weather-api");
        assertNotNull(retrieved);
        assertEquals("weather-api", retrieved.id());
        assertEquals("Weather API", retrieved.name());
        assertEquals(ExternalApiAdapterService.ApiType.WEATHER, retrieved.type());
        assertEquals("https://api.weather.com", retrieved.baseUrl());
        assertEquals("api-key-123", retrieved.apiKey());
    }

    @Test
    void testUnregisterEndpoint() {
        ExternalApiAdapterService.ApiEndpoint endpoint = new ExternalApiAdapterService.ApiEndpoint(
            "weather-api",
            "Weather API",
            ExternalApiAdapterService.ApiType.WEATHER,
            "https://api.weather.com",
            "api-key-123",
            null,
            true
        );

        apiService.registerEndpoint(endpoint);
        assertTrue(apiService.getEndpoint("weather-api") != null);

        apiService.unregisterEndpoint("weather-api");
        assertNull(apiService.getEndpoint("weather-api"));
    }

    @Test
    void testUnregisterNonExistentEndpoint() {
        apiService.unregisterEndpoint("non-existent");
        // Should not throw
    }

    @Test
    void testGetWeatherNoEndpoint() {
        ExternalApiAdapterService.ApiResponse response = apiService.getWeather("Beijing");
        assertFalse(response.success());
        assertEquals("No endpoint configured for WEATHER", response.error());
    }

    @Test
    void testGetNewsNoEndpoint() {
        ExternalApiAdapterService.ApiResponse response = apiService.getNews("tech", 1, 10);
        assertFalse(response.success());
        assertEquals("No endpoint configured for NEWS", response.error());
    }

    @Test
    void testSearchNoEndpoint() {
        ExternalApiAdapterService.ApiResponse response = apiService.search("java", 10);
        assertFalse(response.success());
        assertEquals("No endpoint configured for SEARCH", response.error());
    }

    @Test
    void testTranslateNoEndpoint() {
        ExternalApiAdapterService.ApiResponse response = apiService.translate("hello", "zh");
        assertFalse(response.success());
        assertEquals("No endpoint configured for TRANSLATION", response.error());
    }

    @Test
    void testCallApiNoEndpoint() {
        ExternalApiAdapterService.ApiResponse response = apiService.callApi(
            ExternalApiAdapterService.ApiType.CUSTOM,
            "/custom",
            Map.of("key", "value")
        );
        assertFalse(response.success());
        assertEquals("No endpoint configured for CUSTOM", response.error());
    }

    @Test
    void testCallApiAsyncNoEndpoint() {
        ExternalApiAdapterService.ApiResponse response = apiService.callApiAsync(
            ExternalApiAdapterService.ApiType.WEATHER,
            "/current",
            Map.of("city", "Beijing")
        ).join();

        assertFalse(response.success());
        assertEquals("No endpoint configured for WEATHER", response.error());
    }

    @Test
    void testClearCache() {
        apiService.clearCache();
        // Should not throw
    }

    @Test
    void testClearCacheByType() {
        apiService.clearCache(ExternalApiAdapterService.ApiType.WEATHER);
        // Should not throw
    }

    @Test
    void testGetStats() {
        ExternalApiAdapterService.ApiEndpoint endpoint = new ExternalApiAdapterService.ApiEndpoint(
            "weather-api",
            "Weather API",
            ExternalApiAdapterService.ApiType.WEATHER,
            "https://api.weather.com",
            "api-key-123",
            null,
            true
        );
        apiService.registerEndpoint(endpoint);

        ExternalApiAdapterService.ApiStats stats = apiService.getStats();
        assertEquals(1, stats.totalEndpoints());
        assertEquals(1, stats.enabledEndpoints());
        assertEquals(0, stats.totalCached());
        assertEquals(0, stats.validCached());
    }

    @Test
    void testApiTypes() {
        assertEquals(7, ExternalApiAdapterService.ApiType.values().length);
        assertNotNull(ExternalApiAdapterService.ApiType.valueOf("WEATHER"));
        assertNotNull(ExternalApiAdapterService.ApiType.valueOf("NEWS"));
        assertNotNull(ExternalApiAdapterService.ApiType.valueOf("CALENDAR"));
        assertNotNull(ExternalApiAdapterService.ApiType.valueOf("REMINDER"));
        assertNotNull(ExternalApiAdapterService.ApiType.valueOf("SEARCH"));
        assertNotNull(ExternalApiAdapterService.ApiType.valueOf("TRANSLATION"));
        assertNotNull(ExternalApiAdapterService.ApiType.valueOf("CUSTOM"));
    }

    @Test
    void testApiResponseRecord() {
        ExternalApiAdapterService.ApiResponse response = new ExternalApiAdapterService.ApiResponse(
            true, 200, "{\"temp\": 25}", null, 150, false
        );

        assertTrue(response.success());
        assertEquals(200, response.statusCode());
        assertEquals("{\"temp\": 25}", response.body());
        assertNull(response.error());
        assertEquals(150, response.responseTimeMs());
        assertFalse(response.fromCache());
    }

    @Test
    void testCachedResponseExpired() {
        ExternalApiAdapterService.CachedResponse cached = new ExternalApiAdapterService.CachedResponse(
            "data",
            java.time.Instant.now().minusSeconds(60)
        );

        assertTrue(cached.isExpired());
    }

    @Test
    void testCachedResponseNotExpired() {
        ExternalApiAdapterService.CachedResponse cached = new ExternalApiAdapterService.CachedResponse(
            "data",
            java.time.Instant.now().plusSeconds(60)
        );

        assertFalse(cached.isExpired());
    }
}
