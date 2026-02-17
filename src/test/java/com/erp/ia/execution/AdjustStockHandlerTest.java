package com.erp.ia.execution;

import com.erp.ia.agent.model.ActionType;
import com.erp.ia.agent.model.PlannedAction;
import com.erp.ia.agent.model.RiskLevel;
import com.erp.ia.core.model.Product;
import com.erp.ia.core.model.Stock;
import com.erp.ia.core.model.StockMovement;
import com.erp.ia.core.repository.ProductRepository;
import com.erp.ia.core.service.StockService;
import com.erp.ia.event.EventBus;
import com.erp.ia.event.model.StockLevelChanged;
import com.erp.ia.execution.handler.AdjustStockHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class AdjustStockHandlerTest {

    private StockService stockService;
    private ProductRepository productRepository;
    private EventBus eventBus;
    private AdjustStockHandler handler;

    @BeforeEach
    void setUp() {
        stockService = mock(StockService.class);
        productRepository = mock(ProductRepository.class);
        eventBus = mock(EventBus.class);
        handler = new AdjustStockHandler(stockService, productRepository, eventBus);
    }

    @Test
    void getActionType_returnsAdjustStock() {
        assertEquals(ActionType.ADJUST_STOCK, handler.getActionType());
    }

    @Test
    void handle_adjustsStockAndPublishesEvent() {
        // Arrange
        Product product = new Product("SKU-001", "Widget A", "un");
        product.setId(1L);
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));

        Stock currentStock = new Stock(product, "WH-01", BigDecimal.valueOf(5));
        when(stockService.getStock(1L, "WH-01"))
                .thenReturn(Optional.of(currentStock))
                .thenReturn(Optional.of(new Stock(product, "WH-01", BigDecimal.valueOf(15))));

        StockMovement movement = new StockMovement(product, StockMovement.MovementType.IN, BigDecimal.TEN, "Reposição");
        movement.setId(10L);
        when(stockService.adjustStock(any(), anyString(), any(), any(), anyString(), anyString()))
                .thenReturn(movement);

        PlannedAction action = new PlannedAction(
                ActionType.ADJUST_STOCK,
                Map.of("productId", 1L, "warehouse", "WH-01", "quantity", 10, "movementType", "IN",
                        "reason", "Reposição"),
                RiskLevel.MEDIUM,
                true);

        // Act
        ExecutionResult result = handler.handle(action, "audit-100", "admin");

        // Assert
        assertTrue(result.isSuccess());
        assertEquals(ActionType.ADJUST_STOCK.name(), result.actionType());

        verify(stockService).adjustStock(
                eq(product), eq("WH-01"), eq(StockMovement.MovementType.IN),
                argThat(bd -> bd.compareTo(BigDecimal.TEN) == 0), eq("Reposição"), eq("admin"));

        ArgumentCaptor<StockLevelChanged> eventCaptor = ArgumentCaptor.forClass(StockLevelChanged.class);
        verify(eventBus).publish(eventCaptor.capture());
        StockLevelChanged event = eventCaptor.getValue();
        assertEquals(1L, event.getProductId());
        assertEquals("WH-01", event.getWarehouse());
    }

    @Test
    void handle_failsWhenProductIdMissing() {
        PlannedAction action = new PlannedAction(
                ActionType.ADJUST_STOCK,
                Map.of("warehouse", "WH-01", "quantity", 10),
                RiskLevel.LOW,
                false);

        ExecutionResult result = handler.handle(action, "audit-200", "admin");

        assertFalse(result.isSuccess());
        assertTrue(result.message().contains("productId"));
        verify(stockService, never()).adjustStock(any(), anyString(), any(), any(), anyString(), anyString());
    }

    @Test
    void handle_failsWhenQuantityMissing() {
        PlannedAction action = new PlannedAction(
                ActionType.ADJUST_STOCK,
                Map.of("productId", 1L),
                RiskLevel.LOW,
                false);

        ExecutionResult result = handler.handle(action, "audit-300", "admin");

        assertFalse(result.isSuccess());
        assertTrue(result.message().contains("quantity"));
    }

    @Test
    void handle_failsWhenProductNotFound() {
        when(productRepository.findById(999L)).thenReturn(Optional.empty());

        PlannedAction action = new PlannedAction(
                ActionType.ADJUST_STOCK,
                Map.of("productId", 999L, "quantity", 5),
                RiskLevel.LOW,
                false);

        ExecutionResult result = handler.handle(action, "audit-400", "admin");

        assertFalse(result.isSuccess());
        assertTrue(result.message().contains("999"));
        verify(eventBus, never()).publish(any());
    }

    @Test
    void handle_failsOnInvalidMovementType() {
        PlannedAction action = new PlannedAction(
                ActionType.ADJUST_STOCK,
                Map.of("productId", 1L, "quantity", 5, "movementType", "INVALID_TYPE"),
                RiskLevel.LOW,
                false);

        ExecutionResult result = handler.handle(action, "audit-500", "admin");

        assertFalse(result.isSuccess());
        assertTrue(result.message().contains("INVALID_TYPE"));
    }
}
