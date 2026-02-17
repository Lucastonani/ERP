package com.erp.ia.agent.model;

import com.erp.ia.context.Evidence;
import java.util.List;

/**
 * Response from the agent layer: natural language + structured action plan +
 * evidence + audit ID.
 */
public record AgentResponse(
        String response, // natural language response
        ActionPlan actionPlan, // structured typed actions
        List<Evidence> evidence, // data used for the decision
        String auditId // decision log ID for traceability
) {
}
