package com.erp.ia.event;

import com.erp.ia.event.model.StockLevelChanged;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class InMemoryEventBusTest {

    @Test
    void shouldPublishAndReceiveEvent() throws InterruptedException {
        InMemoryEventBus bus = new InMemoryEventBus();
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<DomainEvent> received = new AtomicReference<>();

        bus.subscribe("STOCK_LEVEL_CHANGED", event -> {
            received.set(event);
            latch.countDown();
        });

        StockLevelChanged event = new StockLevelChanged(
                1L, "WH-01", BigDecimal.TEN, BigDecimal.valueOf(5), "sale");
        bus.publish(event);

        assertTrue(latch.await(2, TimeUnit.SECONDS));
        assertNotNull(received.get());
        assertInstanceOf(StockLevelChanged.class, received.get());
    }

    @Test
    void shouldNotDeliverToUnsubscribedType() throws InterruptedException {
        InMemoryEventBus bus = new InMemoryEventBus();
        CountDownLatch latch = new CountDownLatch(1);

        bus.subscribe("OTHER_TYPE", event -> latch.countDown());

        bus.publish(new StockLevelChanged(1L, "WH", BigDecimal.ONE, BigDecimal.ZERO, "test"));

        assertFalse(latch.await(500, TimeUnit.MILLISECONDS));
    }
}
