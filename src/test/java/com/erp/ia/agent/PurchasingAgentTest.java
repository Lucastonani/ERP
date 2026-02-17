package com.erp.ia.agent;

import com.erp.ia.agent.impl.PurchasingAgent;
import com.erp.ia.agent.model.*;
import com.erp.ia.context.ContextSnapshot;
import com.erp.ia.context.Evidence;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class PurchasingAgentTest {

    private final PurchasingAgent agent = new PurchasingAgent();

    @Test
    void synthesizeShouldGeneratePurchaseOrderDraft() {
        AgentRequest request = new AgentRequest("compra", Map.of(), "default", "default", null, "admin");
        ContextSnapshot context = new ContextSnapshot("purchasing-agent", "compra",
                List.of(new Evidence("StockQueryTool", "query", Map.of("count", 10))));

        AgentResponse response = agent.synthesize(request, context);

        assertNotNull(response.actionPlan());
        assertTrue(response.actionPlan().hasActions());

        PlannedAction action = response.actionPlan().actions().get(0);
        assertEquals(ActionType.DRAFT_PURCHASE_ORDER, action.getType());
        assertTrue(action.isRequiresApproval());
        assertEquals(RiskLevel.MEDIUM, action.getRisk());
        assertNotNull(action.getIdempotencyKey());
    }
}
