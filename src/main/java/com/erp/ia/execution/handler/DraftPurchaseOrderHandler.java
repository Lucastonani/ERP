package com.erp.ia.execution.handler;

import com.erp.ia.agent.model.ActionType;
import com.erp.ia.agent.model.PlannedAction;
import com.erp.ia.core.model.PurchaseOrder;
import com.erp.ia.core.service.PurchaseOrderService;
import com.erp.ia.event.EventBus;
import com.erp.ia.event.model.PurchaseOrderDrafted;
import com.erp.ia.execution.ActionHandler;
import com.erp.ia.execution.ExecutionResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * Handles DRAFT_PURCHASE_ORDER actions.
 * Extracts supplier/reason/tenant from PlannedAction params,
 * delegates to PurchaseOrderService.createDraft(), and publishes
 * a PurchaseOrderDrafted domain event.
 */
@Component
public class DraftPurchaseOrderHandler implements ActionHandler {

    private static final Logger log = LoggerFactory.getLogger(DraftPurchaseOrderHandler.class);

    private final PurchaseOrderService purchaseOrderService;
    private final EventBus eventBus;

    public DraftPurchaseOrderHandler(PurchaseOrderService purchaseOrderService, EventBus eventBus) {
        this.purchaseOrderService = purchaseOrderService;
        this.eventBus = eventBus;
    }

    @Override
    public ActionType getActionType() {
        return ActionType.DRAFT_PURCHASE_ORDER;
    }

    @Override
    public ExecutionResult handle(PlannedAction action, String auditId, String executedBy) {
        Map<String, Object> params = action.getParams();

        String supplier = getStringParam(params, "supplier", "Fornecedor Padrão");
        String reason = getStringParam(params, "reason", "Pedido gerado via agente IA");
        String tenantId = getStringParam(params, "tenantId", "default");
        String storeId = getStringParam(params, "storeId", "default");

        log.info("Creating draft purchase order: supplier={}, reason={}, auditId={}", supplier, reason, auditId);

        try {
            PurchaseOrder order = purchaseOrderService.createDraft(
                    supplier,
                    List.of(), // MVP: draft sem itens; itens serão adicionados quando agente evoluir
                    executedBy,
                    tenantId,
                    storeId);
            order.setNotes(reason + " | auditId=" + auditId);

            log.info("Purchase order created: id={}, orderNumber={}", order.getId(), order.getOrderNumber());

            // Publish domain event
            eventBus.publish(new PurchaseOrderDrafted(
                    order.getId(),
                    order.getOrderNumber(),
                    supplier,
                    auditId));

            return ExecutionResult.success(
                    ActionType.DRAFT_PURCHASE_ORDER.name(),
                    "Pedido de compra criado com sucesso: " + order.getOrderNumber(),
                    Map.of(
                            "orderId", order.getId(),
                            "orderNumber", order.getOrderNumber(),
                            "supplier", supplier,
                            "status", order.getStatus().name()));

        } catch (Exception e) {
            log.error("Failed to create purchase order: {}", e.getMessage(), e);
            return ExecutionResult.failed(ActionType.DRAFT_PURCHASE_ORDER.name(),
                    "Falha ao criar pedido de compra: " + e.getMessage());
        }
    }

    private String getStringParam(Map<String, Object> params, String key, String defaultValue) {
        if (params == null || !params.containsKey(key)) {
            return defaultValue;
        }
        Object value = params.get(key);
        return value != null ? value.toString() : defaultValue;
    }
}
