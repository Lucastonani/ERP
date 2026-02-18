package com.erp.ia.agent.model;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Map;

/**
 * Request from the cognitive interface to the agent layer.
 */
public record AgentRequest(
        String intent,
        @Schema(description = "Context variables (arbitrary key-value pairs)") Map<String, Object> context,
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
