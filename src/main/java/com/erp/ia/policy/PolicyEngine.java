package com.erp.ia.policy;

import com.erp.ia.agent.model.ActionPlan;
import com.erp.ia.agent.model.AgentRequest;
import com.erp.ia.audit.model.DecisionPolicyResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Policy Engine — validates ActionPlan against all registered rules.
 * This is the "ABS brake" of the system.
 */
@Component
public class PolicyEngine {

    private static final Logger log = LoggerFactory.getLogger(PolicyEngine.class);

    private final List<PolicyRule> rules;

    public PolicyEngine(List<PolicyRule> rules) {
        this.rules = rules;
        log.info("PolicyEngine initialized with {} rules", rules.size());
    }

    /**
     * Validate an ActionPlan. Returns PASS only if ALL rules pass.
     */
    public PolicyResult validate(ActionPlan plan, AgentRequest request) {
        List<DecisionPolicyResult> allResults = new ArrayList<>();
        boolean blocked = false;

        for (PolicyRule rule : rules) {
            PolicyResult result = rule.evaluate(plan, request);
            allResults.addAll(result.ruleResults());
            if (!result.isPass()) {
                blocked = true;
                log.warn("Policy BLOCKED by rule '{}': {}", rule.getName(),
                        result.ruleResults().stream()
                                .map(DecisionPolicyResult::getReason)
                                .toList());
            }
        }

        return blocked ? PolicyResult.blocked(allResults) : new PolicyResult("PASS", allResults);
    }
}
