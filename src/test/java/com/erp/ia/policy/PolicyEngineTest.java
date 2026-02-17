package com.erp.ia.policy;

import com.erp.ia.agent.model.*;
import com.erp.ia.policy.rules.ApprovalRequiredRule;
import com.erp.ia.policy.rules.SpendingLimitRule;
import com.erp.ia.policy.rules.EvidenceRequiredRule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class PolicyEngineTest {

    private PolicyEngine policyEngine;

    @BeforeEach
    void setUp() {
        policyEngine = new PolicyEngine(List.of(
                new ApprovalRequiredRule(),
                new SpendingLimitRule(),
                new EvidenceRequiredRule()));
    }

    @Test
    void shouldPassWhenMutatingActionHasApproval() {
        ActionPlan plan = new ActionPlan("test", List.of(
                new PlannedAction(ActionType.DRAFT_PURCHASE_ORDER, Map.of(), RiskLevel.MEDIUM, true)));
        AgentRequest request = new AgentRequest("test", Map.of(), "default", "default", null, "admin");

        PolicyResult result = policyEngine.validate(plan, request);
        assertTrue(result.isPass());
    }

    @Test
    void shouldBlockMutatingActionWithoutApproval() {
        ActionPlan plan = new ActionPlan("test", List.of(
                new PlannedAction(ActionType.DRAFT_PURCHASE_ORDER, Map.of(), RiskLevel.LOW, false)));
        AgentRequest request = new AgentRequest("test", Map.of(), "default", "default", null, "user");

        PolicyResult result = policyEngine.validate(plan, request);
        assertFalse(result.isPass());
    }

    @Test
    void shouldBlockHighRiskActionWithoutUser() {
        ActionPlan plan = new ActionPlan("test", List.of(
                new PlannedAction(ActionType.ADJUST_STOCK, Map.of(), RiskLevel.HIGH, true)));
        AgentRequest request = new AgentRequest("test", Map.of(), "default", "default", null, null);

        PolicyResult result = policyEngine.validate(plan, request);
        assertFalse(result.isPass());
    }

    @Test
    void shouldPassLowRiskActionWithoutUser() {
        ActionPlan plan = new ActionPlan("test", List.of(
                new PlannedAction(ActionType.COMPLIANCE_CHECK, Map.of(), RiskLevel.LOW, false)));
        AgentRequest request = new AgentRequest("test", Map.of(), "default", "default", null, null);

        PolicyResult result = policyEngine.validate(plan, request);
        assertTrue(result.isPass());
    }
}
