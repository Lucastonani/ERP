package com.erp.ia.agent.model;

import java.util.Map;

/**
 * Request from the cognitive interface to the agent layer.
 */
public record AgentRequest(
        String intent,
        Map<String, Object> context,
        String tenantId,
        String storeId,
        String correlationId,
        String user) {
    public AgentRequest {
        if (tenantId == null)
            tenantId = "default";
        if (storeId == null)
            storeId = "default";
        if (context == null)
            context = Map.of();
    }
}
