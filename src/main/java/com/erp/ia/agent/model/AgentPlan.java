package com.erp.ia.agent.model;

import java.util.List;
import java.util.Map;

/**
 * Agent's plan of execution — what tools to call and what data to gather (Phase
 * 1).
 */
public record AgentPlan(
        List<ToolCall> toolCalls) {
    public record ToolCall(
            String toolName,
            String description,
            Map<String, Object> parameters) {
    }

    public static AgentPlan empty() {
        return new AgentPlan(List.of());
    }
}
