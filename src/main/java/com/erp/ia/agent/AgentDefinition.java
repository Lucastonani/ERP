package com.erp.ia.agent;

import com.erp.ia.agent.model.AgentPlan;
import com.erp.ia.agent.model.AgentRequest;
import com.erp.ia.agent.model.AgentResponse;
import com.erp.ia.context.ContextSnapshot;

import java.util.Set;

/**
 * Contract for all AI agents. Two-phase lifecycle:
 * 1. plan() — determine what tools/data are needed
 * 2. synthesize() — given evidence, produce response + action plan
 */
public interface AgentDefinition {

    String getName();

    String getDescription();

    /** Which intents this agent can handle. */
    Set<String> getSupportedIntents();

    /** Phase 1: determine what tools to call and what data to gather. */
    AgentPlan plan(AgentRequest request);

    /** Phase 2: given assembled context, produce the response + action plan. */
    AgentResponse synthesize(AgentRequest request, ContextSnapshot context);

    /**
     * Whether this agent should use the LLM for synthesize.
     * Default true. Override to false for pure-query agents
     * that don't need AI reasoning.
     */
    default boolean usesLlm() {
        return true;
    }
}
