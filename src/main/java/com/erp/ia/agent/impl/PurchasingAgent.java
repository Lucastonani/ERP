package com.erp.ia.agent.impl;

import com.erp.ia.agent.AgentDefinition;
import com.erp.ia.agent.model.*;
import com.erp.ia.context.ContextSnapshot;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Purchasing Agent — drafts purchase orders, suggests suppliers.
 */
@Component
public class PurchasingAgent implements AgentDefinition {

    @Override
    public String getName() {
        return "purchasing-agent";
    }

    @Override
    public String getDescription() {
        return "Handles purchase order creation and supplier suggestions";
    }

    @Override
    public Set<String> getSupportedIntents() {
        return Set.of("compra", "purchase", "pedido", "order", "supplier", "fornecedor");
    }

    @Override
    public AgentPlan plan(AgentRequest request) {
        return new AgentPlan(List.of(
                new AgentPlan.ToolCall("StockQueryTool", "Check current stock levels",
                        Map.of("tenantId", request.tenantId())),
                new AgentPlan.ToolCall("ProductQueryTool", "List available products",
                        Map.of("tenantId", request.tenantId()))));
    }

    @Override
    public AgentResponse synthesize(AgentRequest request, ContextSnapshot context) {
        StringBuilder response = new StringBuilder();
        response.append("Analisei os dados de estoque e produtos disponíveis. ");

        List<PlannedAction> actions = new ArrayList<>();
        actions.add(new PlannedAction(
                ActionType.DRAFT_PURCHASE_ORDER,
                Map.of(
                        "intent", request.intent(),
                        "analysisBase", "stock_levels + product_catalog"),
                RiskLevel.MEDIUM,
                true // purchase orders always require approval
        ));
        response.append("Sugiro criar um pedido de compra com base nos níveis atuais de estoque. ");
        response.append("O pedido requer aprovação antes da execução.");

        ActionPlan actionPlan = new ActionPlan(response.toString(), actions);

        return new AgentResponse(
                response.toString(),
                actionPlan,
                context.getEvidences(),
                null);
    }
}
