package com.erp.ia.agent.impl;

import com.erp.ia.agent.AgentDefinition;
import com.erp.ia.agent.model.*;
import com.erp.ia.context.ContextSnapshot;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Inventory Agent — handles stock queries, reorder suggestions, and stock
 * analysis.
 */
@Component
public class InventoryAgent implements AgentDefinition {

    @Override
    public String getName() {
        return "inventory-agent";
    }

    @Override
    public String getDescription() {
        return "Manages stock queries, reorder detection, and stock adjustments";
    }

    @Override
    public Set<String> getSupportedIntents() {
        return Set.of("stock", "estoque", "inventory", "reorder", "reposicao");
    }

    @Override
    public AgentPlan plan(AgentRequest request) {
        String intent = request.intent().toLowerCase();

        List<AgentPlan.ToolCall> toolCalls = new ArrayList<>();

        // Always query stock levels
        toolCalls.add(new AgentPlan.ToolCall(
                "StockQueryTool",
                "Query current stock levels",
                Map.of("tenantId", request.tenantId())));

        if (intent.contains("reorder") || intent.contains("reposicao")) {
            toolCalls.add(new AgentPlan.ToolCall(
                    "ProductQueryTool",
                    "Find products below minimum stock",
                    Map.of("belowMinimum", true, "tenantId", request.tenantId())));
        }

        return new AgentPlan(toolCalls);
    }

    @Override
    public AgentResponse synthesize(AgentRequest request, ContextSnapshot context) {
        String intent = request.intent().toLowerCase();

        StringBuilder response = new StringBuilder();
        List<PlannedAction> actions = new ArrayList<>();

        if (intent.contains("reorder") || intent.contains("reposicao")) {
            response.append("Analisei os níveis de estoque. ");
            if (context.getEvidences().isEmpty()) {
                response.append("Nenhum dado de estoque encontrado.");
            } else {
                response.append("Encontrei itens que precisam de reposição. ");
                response.append("Recomendo criar um pedido de compra para os itens abaixo do mínimo.");
                actions.add(new PlannedAction(
                        ActionType.DRAFT_PURCHASE_ORDER,
                        Map.of("reason", "Reposição automática - itens abaixo do mínimo"),
                        RiskLevel.MEDIUM,
                        true));
            }
        } else {
            response.append("Aqui está o resumo do estoque atual. ");
            if (context.hasEvidence()) {
                response.append("Dados consultados com ").append(context.getEvidences().size()).append(" fontes.");
            }
        }

        ActionPlan actionPlan = new ActionPlan(response.toString(), actions);

        return new AgentResponse(
                response.toString(),
                actionPlan,
                context.getEvidences(),
                null // audit ID is set by orchestrator
        );
    }
}
