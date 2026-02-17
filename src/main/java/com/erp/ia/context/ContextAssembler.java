package com.erp.ia.context;

import com.erp.ia.agent.model.AgentPlan;
import com.erp.ia.agent.model.AgentRequest;
import com.erp.ia.tool.ToolExecutor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Assembles context (evidence) for an agent by executing the tools specified in
 * its plan.
 * Separated from the orchestrator for testability and single responsibility.
 */
@Component
public class ContextAssembler {

    private final ToolExecutor toolExecutor;

    public ContextAssembler(ToolExecutor toolExecutor) {
        this.toolExecutor = toolExecutor;
    }

    /**
     * Execute the agent's plan: call each tool and collect evidence.
     */
    public ContextSnapshot assemble(AgentRequest request, String agentName, AgentPlan plan) {
        List<Evidence> evidences = new ArrayList<>();

        for (AgentPlan.ToolCall toolCall : plan.toolCalls()) {
            Object result = toolExecutor.executeTool(toolCall.toolName(), toolCall.parameters());
            evidences.add(new Evidence(toolCall.toolName(), toolCall.description(), result));
        }

        return new ContextSnapshot(agentName, request.intent(), evidences);
    }
}
