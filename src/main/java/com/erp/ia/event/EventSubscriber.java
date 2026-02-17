package com.erp.ia.event;

@FunctionalInterface
public interface EventSubscriber {
    void onEvent(DomainEvent event);
}
