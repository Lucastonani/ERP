package com.erp.ia.policy.rules;

import com.erp.ia.agent.model.*;
import com.erp.ia.audit.model.DecisionPolicyResult;
import com.erp.ia.policy.PolicyResult;
import com.erp.ia.policy.PolicyRule;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Rule: HIGH and CRITICAL risk actions cannot proceed without ADMIN role.
 * (MVP: checks user field in request. In production: integrate with Spring
 * Security principal.)
 */
@Component
public class SpendingLimitRule implements PolicyRule {

    @Override
    public String getName() {
        return "SpendingLimitRule";
    }

    @Override
    public PolicyResult evaluate(ActionPlan plan, AgentRequest request) {
        List<DecisionPolicyResult> results = new ArrayList<>();
        boolean blocked = false;

        for (PlannedAction action : plan.actions()) {
            if (action.getRisk() == RiskLevel.HIGH || action.getRisk() == RiskLevel.CRITICAL) {
                // In MVP, only check if user is present. In prod, check ADMIN role.
                if (request.user() == null || request.user().isBlank()) {
                    results.add(new DecisionPolicyResult(getName(), "BLOCKED",
                            "High-risk action " + action.getType() + " requires authenticated ADMIN user"));
                    blocked = true;
                } else {
                    results.add(new DecisionPolicyResult(getName(), "PASS",
                            "User " + request.user() + " present for high-risk action"));
                }
            } else {
                results.add(new DecisionPolicyResult(getName(), "PASS", null));
            }
        }

        return blocked ? PolicyResult.blocked(results) : new PolicyResult("PASS", results);
    }
}
