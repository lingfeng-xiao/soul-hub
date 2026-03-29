package com.lingfeng.sprite.runtime;

import com.lingfeng.sprite.runtime.event.DomainEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.function.Consumer;

/**
 * DomainEventBus - Singleton event bus for in-process publish-subscribe communication.
 *
 * This bus decouples components within the Sprite runtime, enabling event-driven
 * architecture for cognitive cycles, memory operations, and action dispatching.
 *
 * Supported event types:
 * - StimulusCaptured: When new sensory input is captured
 * - CycleStarted: When a new cognition cycle begins
 * - MemoryRetrieved: When memories are retrieved from storage
 * - DecisionSelected: When a decision is made by the decision engine
 * - ActionDispatched: When an action is dispatched for execution
 */
public final class DomainEventBus {

    private static final Logger logger = LoggerFactory.getLogger(DomainEventBus.class);
    private static final DomainEventBus INSTANCE = new DomainEventBus();

    private final Map<Class<? extends DomainEvent>, Set<Consumer<? extends DomainEvent>>> handlers;
    private final Map<Class<? extends DomainEvent>, Set<String>> subscriptionIds;

    private DomainEventBus() {
        this.handlers = new ConcurrentHashMap<>();
        this.subscriptionIds = new ConcurrentHashMap<>();
    }

    /**
     * Get the singleton instance of DomainEventBus.
     */
    public static DomainEventBus getInstance() {
        return INSTANCE;
    }

    /**
     * Subscribe to events of a specific type.
     *
     * @param eventType The class of events to subscribe to
     * @param handler   The consumer that will handle matching events
     * @param <T>       The event type
     * @return A subscription ID that can be used to unsubscribe
     */
    public <T extends DomainEvent> String subscribe(Class<T> eventType, Consumer<T> handler) {
        String subscriptionId = UUID.randomUUID().toString();

        handlers.computeIfAbsent(eventType, k -> new CopyOnWriteArraySet<>()).add(handler);
        subscriptionIds.computeIfAbsent(eventType, k -> new CopyOnWriteArraySet<>()).add(subscriptionId);

        logger.debug("Subscribed handler to event type {} with subscriptionId {}", eventType.getSimpleName(), subscriptionId);
        return subscriptionId;
    }

    /**
     * Unsubscribe a specific handler using the subscription ID.
     *
     * @param eventType     The event type to unsubscribe from
     * @param subscriptionId The subscription ID returned from subscribe
     * @return true if the subscription was found and removed, false otherwise
     */
    public boolean unsubscribe(Class<? extends DomainEvent> eventType, String subscriptionId) {
        Set<Consumer<? extends DomainEvent>> eventHandlers = handlers.get(eventType);
        Set<String> ids = subscriptionIds.get(eventType);

        if (eventHandlers == null || ids == null) {
            return false;
        }

        boolean removed = ids.remove(subscriptionId);
        if (removed && ids.isEmpty()) {
            handlers.remove(eventType);
            subscriptionIds.remove(eventType);
        }

        logger.debug("Unsubscribed subscriptionId {} from event type {}", subscriptionId, eventType.getSimpleName());
        return removed;
    }

    /**
     * Unsubscribe all handlers for a specific event type.
     *
     * @param eventType The event type to unsubscribe from
     */
    public void unsubscribeAll(Class<? extends DomainEvent> eventType) {
        handlers.remove(eventType);
        subscriptionIds.remove(eventType);
        logger.debug("Unsubscribed all handlers from event type {}", eventType.getSimpleName());
    }

    /**
     * Publish an event to all subscribed handlers.
     *
     * @param event The event to publish
     * @param <T>   The event type
     */
    @SuppressWarnings("unchecked")
    public <T extends DomainEvent> void publish(T event) {
        Class<? extends DomainEvent> eventType = event.getClass();
        Set<Consumer<? extends DomainEvent>> eventHandlers = handlers.get(eventType);

        if (eventHandlers == null || eventHandlers.isEmpty()) {
            logger.trace("No handlers subscribed for event type {}", eventType.getSimpleName());
            return;
        }

        logger.debug("Publishing event {} with id {} for cycle {}",
            eventType.getSimpleName(), event.getEventId(), event.getCycleId());

        for (Consumer<? extends DomainEvent> handler : eventHandlers) {
            try {
                ((Consumer<T>) handler).accept(event);
            } catch (Exception e) {
                logger.error("Error handling event {} in subscription: {}",
                    eventType.getSimpleName(), e.getMessage(), e);
            }
        }
    }

    /**
     * Check if there are any subscribers for a specific event type.
     *
     * @param eventType The event type to check
     * @return true if there are subscribers, false otherwise
     */
    public boolean hasSubscribers(Class<? extends DomainEvent> eventType) {
        Set<Consumer<? extends DomainEvent>> eventHandlers = handlers.get(eventType);
        return eventHandlers != null && !eventHandlers.isEmpty();
    }

    /**
     * Get the number of subscribers for a specific event type.
     *
     * @param eventType The event type to check
     * @return The number of subscribers
     */
    public int getSubscriberCount(Class<? extends DomainEvent> eventType) {
        Set<Consumer<? extends DomainEvent>> eventHandlers = handlers.get(eventType);
        return eventHandlers == null ? 0 : eventHandlers.size();
    }
}
