package com.erp.ia.agent;

import com.erp.ia.agent.impl.InventoryAgent;
import com.erp.ia.agent.model.*;
import com.erp.ia.context.ContextSnapshot;
import com.erp.ia.context.Evidence;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class InventoryAgentTest {

    private final InventoryAgent agent = new InventoryAgent();

    @Test
    void planShouldIncludeStockQueryTool() {
        AgentRequest request = new AgentRequest("stock", Map.of(), "default", "default", null, null);
        AgentPlan plan = agent.plan(request);

        assertFalse(plan.toolCalls().isEmpty());
        assertTrue(plan.toolCalls().stream().anyMatch(tc -> tc.toolName().equals("StockQueryTool")));
    }

    @Test
    void planForReorderShouldIncludeProductQueryTool() {
        AgentRequest request = new AgentRequest("reorder", Map.of(), "default", "default", null, null);
        AgentPlan plan = agent.plan(request);

        assertEquals(2, plan.toolCalls().size());
        assertTrue(plan.toolCalls().stream().anyMatch(tc -> tc.toolName().equals("ProductQueryTool")));
    }

    @Test
    void synthesizeShouldProduceActionPlanForReorder() {
        AgentRequest request = new AgentRequest("reorder", Map.of(), "default", "default", null, null);
        ContextSnapshot context = new ContextSnapshot("inventory-agent", "reorder",
                List.of(new Evidence("StockQueryTool", "query stock", Map.of("items", 5))));

        AgentResponse response = agent.synthesize(request, context);

        assertNotNull(response.actionPlan());
        assertTrue(response.actionPlan().hasActions());
        assertEquals(ActionType.DRAFT_PURCHASE_ORDER, response.actionPlan().actions().get(0).getType());
        assertTrue(response.actionPlan().actions().get(0).isRequiresApproval());
    }

    @Test
    void synthesizeShouldReturnResponseForStockQuery() {
        AgentRequest request = new AgentRequest("stock", Map.of(), "default", "default", null, null);
        ContextSnapshot context = new ContextSnapshot("inventory-agent", "stock",
                List.of(new Evidence("StockQueryTool", "query", Map.of("count", 10))));

        AgentResponse response = agent.synthesize(request, context);

        assertNotNull(response.response());
        assertFalse(response.response().isEmpty());
    }
}
