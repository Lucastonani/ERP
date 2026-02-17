package com.erp.ia.policy;

import com.erp.ia.audit.model.DecisionPolicyResult;
import java.util.List;

/**
 * Result of policy validation. Contains per-rule results and overall status.
 */
public record PolicyResult(
        String status, // PASS or BLOCKED
        List<DecisionPolicyResult> ruleResults) {
    public boolean isPass() {
        return "PASS".equals(status);
    }

    public static PolicyResult pass() {
        return new PolicyResult("PASS", List.of());
    }

    public static PolicyResult blocked(List<DecisionPolicyResult> results) {
        return new PolicyResult("BLOCKED", results);
    }
}
