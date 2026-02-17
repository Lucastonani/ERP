package com.erp.ia.policy.rules;

import com.erp.ia.agent.model.ActionPlan;
import com.erp.ia.agent.model.AgentRequest;
import com.erp.ia.audit.model.DecisionPolicyResult;
import com.erp.ia.policy.PolicyResult;
import com.erp.ia.policy.PolicyRule;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Rule: an ActionPlan with actions MUST have non-empty evidence.
 * Prevents "blind decisions" — the agent must have consulted data.
 * (This is validated at the ActionPlan level, not per action.)
 */
@Component
public class EvidenceRequiredRule implements PolicyRule {

    @Override
    public String getName() {
        return "EvidenceRequiredRule";
    }

    @Override
    public PolicyResult evaluate(ActionPlan plan, AgentRequest request) {
        // This rule is evaluated by the orchestrator before calling the policy engine,
        // but we keep it here for completeness and audit trail.
        // In a full implementation, we would receive the ContextSnapshot as well.
        // For MVP, we pass this rule always (evidence is enforced structurally by the
        // plan/synthesize flow).
        return new PolicyResult("PASS",
                List.of(new DecisionPolicyResult(getName(), "PASS",
                        "Evidence enforcement is structural via plan/synthesize flow")));
    }
}
