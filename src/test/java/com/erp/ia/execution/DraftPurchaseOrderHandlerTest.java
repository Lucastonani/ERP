package com.erp.ia.execution;

import com.erp.ia.agent.model.ActionType;
import com.erp.ia.agent.model.PlannedAction;
import com.erp.ia.agent.model.RiskLevel;
import com.erp.ia.core.model.PurchaseOrder;
import com.erp.ia.core.service.PurchaseOrderService;
import com.erp.ia.event.EventBus;
import com.erp.ia.event.model.PurchaseOrderDrafted;
import com.erp.ia.execution.handler.DraftPurchaseOrderHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class DraftPurchaseOrderHandlerTest {

    private PurchaseOrderService purchaseOrderService;
    private EventBus eventBus;
    private DraftPurchaseOrderHandler handler;

    @BeforeEach
    void setUp() {
        purchaseOrderService = mock(PurchaseOrderService.class);
        eventBus = mock(EventBus.class);
        handler = new DraftPurchaseOrderHandler(purchaseOrderService, eventBus);
    }

    @Test
    void getActionType_returnsDraftPurchaseOrder() {
        assertEquals(ActionType.DRAFT_PURCHASE_ORDER, handler.getActionType());
    }

    @Test
    void handle_createsOrderAndPublishesEvent() {
        // Arrange
        PurchaseOrder mockOrder = new PurchaseOrder("PO-ABC12345", "Fornecedor Teste");
        mockOrder.setId(1L);
        when(purchaseOrderService.createDraft(anyString(), anyList(), anyString(), anyString(), anyString()))
                .thenReturn(mockOrder);

        PlannedAction action = new PlannedAction(
                ActionType.DRAFT_PURCHASE_ORDER,
                Map.of("supplier", "Fornecedor Teste", "reason", "Reposição", "tenantId", "t1", "storeId", "s1"),
                RiskLevel.MEDIUM,
                true);

        // Act
        ExecutionResult result = handler.handle(action, "audit-123", "admin");

        // Assert
        assertTrue(result.isSuccess());
        assertEquals(ActionType.DRAFT_PURCHASE_ORDER.name(), result.actionType());
        assertNotNull(result.message());
        assertTrue(result.message().contains("PO-ABC12345"));

        // Verify core service was called with correct params
        verify(purchaseOrderService).createDraft(
                eq("Fornecedor Teste"),
                eq(List.of()),
                eq("admin"),
                eq("t1"),
                eq("s1"));

        // Verify event was published
        ArgumentCaptor<PurchaseOrderDrafted> eventCaptor = ArgumentCaptor.forClass(PurchaseOrderDrafted.class);
        verify(eventBus).publish(eventCaptor.capture());
        PurchaseOrderDrafted event = eventCaptor.getValue();
        assertEquals(1L, event.getOrderId());
        assertEquals("PO-ABC12345", event.getOrderNumber());
        assertEquals("Fornecedor Teste", event.getSupplier());
        assertEquals("audit-123", event.getAuditId());
    }

    @Test
    void handle_usesDefaultsWhenParamsAreMissing() {
        // Arrange
        PurchaseOrder mockOrder = new PurchaseOrder("PO-DEFAULT1", "Fornecedor Padrão");
        mockOrder.setId(2L);
        when(purchaseOrderService.createDraft(anyString(), anyList(), anyString(), anyString(), anyString()))
                .thenReturn(mockOrder);

        PlannedAction action = new PlannedAction(
                ActionType.DRAFT_PURCHASE_ORDER,
                Map.of(), // no params
                RiskLevel.LOW,
                false);

        // Act
        ExecutionResult result = handler.handle(action, "audit-456", "user1");

        // Assert
        assertTrue(result.isSuccess());

        // Should use default supplier
        verify(purchaseOrderService).createDraft(
                eq("Fornecedor Padrão"),
                eq(List.of()),
                eq("user1"),
                eq("default"),
                eq("default"));
    }

    @Test
    void handle_returnsFailedOnException() {
        // Arrange
        when(purchaseOrderService.createDraft(anyString(), anyList(), anyString(), anyString(), anyString()))
                .thenThrow(new RuntimeException("DB connection lost"));

        PlannedAction action = new PlannedAction(
                ActionType.DRAFT_PURCHASE_ORDER,
                Map.of("supplier", "Teste"),
                RiskLevel.LOW,
                false);

        // Act
        ExecutionResult result = handler.handle(action, "audit-789", "admin");

        // Assert
        assertFalse(result.isSuccess());
        assertEquals("FAILED", result.status());
        assertTrue(result.message().contains("DB connection lost"));

        // Event should NOT be published on failure
        verify(eventBus, never()).publish(any());
    }
}
