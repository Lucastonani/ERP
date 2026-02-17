package com.erp.ia.event;

import java.time.Instant;
import java.util.UUID;

/**
 * Base class for all domain events. Typed, versioned, with correlation ID.
 */
public abstract class DomainEvent {

    private final String eventId;
    private final String eventType;
    private final int eventVersion;
    private final Instant occurredAt;
    private String correlationId;

    protected DomainEvent(String eventType, int eventVersion) {
        this.eventId = UUID.randomUUID().toString();
        this.eventType = eventType;
        this.eventVersion = eventVersion;
        this.occurredAt = Instant.now();
    }

    public String getEventId() {
        return eventId;
    }

    public String getEventType() {
        return eventType;
    }

    public int getEventVersion() {
        return eventVersion;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }

    public String getCorrelationId() {
        return correlationId;
    }

    public void setCorrelationId(String correlationId) {
        this.correlationId = correlationId;
    }
}
