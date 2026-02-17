package com.erp.ia.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

/**
 * In-memory event bus for development. Async delivery via executor.
 */
@Component
public class InMemoryEventBus implements EventBus {

    private static final Logger log = LoggerFactory.getLogger(InMemoryEventBus.class);

    private final Map<String, List<EventSubscriber>> subscribers = new ConcurrentHashMap<>();
    private final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();

    @Override
    public void publish(DomainEvent event) {
        log.info("Publishing event: {} (id={})", event.getEventType(), event.getEventId());
        List<EventSubscriber> subs = subscribers.getOrDefault(event.getEventType(), List.of());
        for (EventSubscriber sub : subs) {
            executor.submit(() -> {
                try {
                    sub.onEvent(event);
                } catch (Exception e) {
                    log.error("Error processing event {} by subscriber: {}", event.getEventId(), e.getMessage(), e);
                }
            });
        }
    }

    @Override
    public void subscribe(String eventType, EventSubscriber subscriber) {
        subscribers.computeIfAbsent(eventType, k -> new CopyOnWriteArrayList<>()).add(subscriber);
        log.info("Subscriber registered for event type: {}", eventType);
    }
}
