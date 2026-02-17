package com.erp.ia.policy.rules;

import com.erp.ia.agent.model.ActionPlan;
import com.erp.ia.agent.model.AgentRequest;
import com.erp.ia.agent.model.PlannedAction;
import com.erp.ia.audit.model.DecisionPolicyResult;
import com.erp.ia.policy.PolicyResult;
import com.erp.ia.policy.PolicyRule;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Rule: actions that modify state MUST have requiresApproval = true.
 */
@Component
public class ApprovalRequiredRule implements PolicyRule {

    private static final java.util.Set<String> MUTATING_ACTIONS = java.util.Set.of(
            "DRAFT_PURCHASE_ORDER", "ADJUST_STOCK");

    @Override
    public String getName() {
        return "ApprovalRequiredRule";
    }

    @Override
    public PolicyResult evaluate(ActionPlan plan, AgentRequest request) {
        List<DecisionPolicyResult> results = new ArrayList<>();
        boolean blocked = false;

        for (PlannedAction action : plan.actions()) {
            if (MUTATING_ACTIONS.contains(action.getType().name()) && !action.isRequiresApproval()) {
                results.add(new DecisionPolicyResult(getName(), "BLOCKED",
                        "Action " + action.getType() + " must require approval"));
                blocked = true;
            } else {
                results.add(new DecisionPolicyResult(getName(), "PASS", null));
            }
        }

        return blocked ? PolicyResult.blocked(results) : new PolicyResult("PASS", results);
    }
}
