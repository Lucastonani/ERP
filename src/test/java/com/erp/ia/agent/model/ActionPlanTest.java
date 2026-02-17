package com.erp.ia.agent.model;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ActionPlanTest {

    @Test
    void shouldDetectRequiresApproval() {
        ActionPlan plan = new ActionPlan("test", List.of(
                new PlannedAction(ActionType.ADJUST_STOCK, Map.of(), RiskLevel.LOW, false),
                new PlannedAction(ActionType.DRAFT_PURCHASE_ORDER, Map.of(), RiskLevel.MEDIUM, true)));
        assertTrue(plan.requiresApproval());
    }

    @Test
    void shouldDetectNoApprovalNeeded() {
        ActionPlan plan = new ActionPlan("test", List.of(
                new PlannedAction(ActionType.QUERY_STOCK, Map.of(), RiskLevel.LOW, false)));
        assertFalse(plan.requiresApproval());
    }

    @Test
    void emptyPlanShouldNotHaveActions() {
        ActionPlan plan = ActionPlan.empty("nothing to do");
        assertFalse(plan.hasActions());
    }

    @Test
    void plannedActionShouldHaveIdempotencyKey() {
        PlannedAction action = new PlannedAction(ActionType.ADJUST_STOCK, Map.of(), RiskLevel.LOW, false);
        assertNotNull(action.getIdempotencyKey());
        assertEquals(36, action.getIdempotencyKey().length()); // UUID format
    }
}
