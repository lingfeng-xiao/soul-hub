package com.lingfeng.sprite.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * WebhookService 单元测试
 */
class WebhookServiceTest {

    private WebhookService webhookService;

    @BeforeEach
    void setUp() {
        webhookService = new WebhookService();
    }

    @Test
    void testRegisterEndpoint() {
        WebhookService.WebhookEndpoint endpoint = webhookService.registerEndpoint(
            "Test Webhook",
            "https://example.com/webhook",
            "secret123",
            List.of(WebhookService.EventType.SPRITE_STARTED, WebhookService.EventType.SPRITE_STOPPED)
        );

        assertNotNull(endpoint);
        assertEquals("Test Webhook", endpoint.name());
        assertEquals(URI.create("https://example.com/webhook"), endpoint.url());
        assertEquals("secret123", endpoint.secret());
        assertTrue(endpoint.enabled());
        assertEquals(2, endpoint.subscribedEvents().size());
        assertTrue(endpoint.subscribedEvents().contains(WebhookService.EventType.SPRITE_STARTED));
        assertTrue(endpoint.subscribedEvents().contains(WebhookService.EventType.SPRITE_STOPPED));
    }

    @Test
    void testUnregisterEndpoint() {
        WebhookService.WebhookEndpoint endpoint = webhookService.registerEndpoint(
            "Test Webhook",
            "https://example.com/webhook",
            "secret123",
            List.of(WebhookService.EventType.SPRITE_STARTED)
        );

        String endpointId = endpoint.id();
        assertTrue(webhookService.unregisterEndpoint(endpointId));
        assertNull(webhookService.getEndpoint(endpointId));
    }

    @Test
    void testUnregisterNonExistentEndpoint() {
        assertFalse(webhookService.unregisterEndpoint("non-existent-id"));
    }

    @Test
    void testGetEndpoint() {
        WebhookService.WebhookEndpoint endpoint = webhookService.registerEndpoint(
            "Test Webhook",
            "https://example.com/webhook",
            "secret123",
            List.of(WebhookService.EventType.SPRITE_STARTED)
        );

        WebhookService.WebhookEndpoint retrieved = webhookService.getEndpoint(endpoint.id());
        assertNotNull(retrieved);
        assertEquals(endpoint.id(), retrieved.id());
        assertEquals(endpoint.name(), retrieved.name());
    }

    @Test
    void testGetAllEndpoints() {
        webhookService.registerEndpoint(
            "Webhook 1",
            "https://example.com/webhook1",
            "secret1",
            List.of(WebhookService.EventType.SPRITE_STARTED)
        );
        webhookService.registerEndpoint(
            "Webhook 2",
            "https://example.com/webhook2",
            "secret2",
            List.of(WebhookService.EventType.SPRITE_STOPPED)
        );

        List<WebhookService.WebhookEndpoint> endpoints = webhookService.getAllEndpoints();
        assertEquals(2, endpoints.size());
    }

    @Test
    void testGetEnabledEndpoints() {
        webhookService.registerEndpoint(
            "Enabled Webhook",
            "https://example.com/enabled",
            "secret",
            List.of(WebhookService.EventType.SPRITE_STARTED)
        );
        WebhookService.WebhookEndpoint disabled = webhookService.registerEndpoint(
            "Disabled Webhook",
            "https://example.com/disabled",
            "secret",
            List.of(WebhookService.EventType.SPRITE_STOPPED)
        );
        webhookService.updateEndpoint(disabled.id(), false);

        List<WebhookService.WebhookEndpoint> enabledEndpoints = webhookService.getEnabledEndpoints();
        assertEquals(1, enabledEndpoints.size());
        assertEquals("Enabled Webhook", enabledEndpoints.get(0).name());
    }

    @Test
    void testUpdateEndpoint() {
        WebhookService.WebhookEndpoint endpoint = webhookService.registerEndpoint(
            "Test Webhook",
            "https://example.com/webhook",
            "secret123",
            List.of(WebhookService.EventType.SPRITE_STARTED)
        );

        webhookService.updateEndpoint(endpoint.id(), false);

        WebhookService.WebhookEndpoint updated = webhookService.getEndpoint(endpoint.id());
        assertFalse(updated.enabled());
    }

    @Test
    void testTriggerEventNoSubscribers() {
        List<WebhookService.DeliveryResult> results = webhookService.triggerEvent(
            WebhookService.EventType.SPRITE_STARTED,
            Map.of("test", "value")
        );
        assertTrue(results.isEmpty());
    }

    @Test
    void testTriggerEventWithPayload() {
        webhookService.registerEndpoint(
            "Test Webhook",
            "https://example.com/webhook",
            "secret123",
            List.of(WebhookService.EventType.SPRITE_STARTED)
        );

        List<WebhookService.DeliveryResult> results = webhookService.triggerEvent(
            WebhookService.EventType.SPRITE_STARTED,
            Map.of("message", "Sprite has started")
        );
        // Since we're not mocking HTTP, delivery will fail, but it should still return a result
        assertEquals(1, results.size());
        assertFalse(results.get(0).success());
        assertEquals("Test Webhook", webhookService.getEndpoint(results.get(0).endpointId()).name());
    }

    @Test
    void testTriggerEventSimplified() {
        webhookService.registerEndpoint(
            "Test Webhook",
            "https://example.com/webhook",
            "secret123",
            List.of(WebhookService.EventType.SPRITE_STOPPED)
        );

        List<WebhookService.DeliveryResult> results = webhookService.triggerEvent(
            WebhookService.EventType.SPRITE_STOPPED
        );
        assertEquals(1, results.size());
    }

    @Test
    void testTriggerEventWithMessage() {
        webhookService.registerEndpoint(
            "Test Webhook",
            "https://example.com/webhook",
            "secret123",
            List.of(WebhookService.EventType.EMOTION_CHANGED)
        );

        List<WebhookService.DeliveryResult> results = webhookService.triggerEvent(
            WebhookService.EventType.EMOTION_CHANGED,
            "Owner mood changed to happy"
        );
        assertEquals(1, results.size());
    }

    @Test
    void testGetStats() {
        webhookService.registerEndpoint(
            "Webhook 1",
            "https://example.com/webhook1",
            "secret1",
            List.of(WebhookService.EventType.SPRITE_STARTED)
        );
        webhookService.registerEndpoint(
            "Webhook 2",
            "https://example.com/webhook2",
            "secret2",
            List.of(WebhookService.EventType.SPRITE_STOPPED)
        );

        WebhookService.WebhookStats stats = webhookService.getStats();
        assertEquals(2, stats.totalEndpoints());
        assertEquals(2, stats.enabledEndpoints());
        assertEquals(0, stats.totalSuccess());
        assertEquals(0, stats.totalFailures());
    }

    @Test
    void testEventTypes() {
        assertEquals(10, WebhookService.EventType.values().length);
        assertNotNull(WebhookService.EventType.valueOf("SPRITE_STARTED"));
        assertNotNull(WebhookService.EventType.valueOf("SPRITE_STOPPED"));
        assertNotNull(WebhookService.EventType.valueOf("EMOTION_CHANGED"));
        assertNotNull(WebhookService.EventType.valueOf("DECISION_MADE"));
        assertNotNull(WebhookService.EventType.valueOf("ACTION_EXECUTED"));
        assertNotNull(WebhookService.EventType.valueOf("MEMORY_CONSOLIDATED"));
        assertNotNull(WebhookService.EventType.valueOf("EVOLUTION_TRIGGERED"));
        assertNotNull(WebhookService.EventType.valueOf("ERROR_OCCURRED"));
        assertNotNull(WebhookService.EventType.valueOf("OWNER_INTERACTION"));
        assertNotNull(WebhookService.EventType.valueOf("PROACTIVE_MESSAGE"));
    }
}
