package com.erp.ia.policy;

import com.erp.ia.agent.model.ActionPlan;
import com.erp.ia.agent.model.AgentRequest;

/**
 * Policy rule interface. Each rule evaluates an ActionPlan and returns PASS or
 * BLOCKED.
 */
public interface PolicyRule {
    String getName();

    PolicyResult evaluate(ActionPlan plan, AgentRequest request);
}
