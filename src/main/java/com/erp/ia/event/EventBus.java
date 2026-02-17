package com.erp.ia.event;

/**
 * Port for event bus. In-memory for dev, Kafka for production.
 */
public interface EventBus {
    void publish(DomainEvent event);

    void subscribe(String eventType, EventSubscriber subscriber);
}
