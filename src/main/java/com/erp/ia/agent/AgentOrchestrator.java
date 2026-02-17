package com.erp.ia.agent;

import com.erp.ia.agent.model.*;
import com.erp.ia.audit.DecisionLogService;
import com.erp.ia.audit.model.DecisionLog;
import com.erp.ia.audit.model.DecisionToolCall;
import com.erp.ia.context.ContextAssembler;
import com.erp.ia.context.ContextSnapshot;
import com.erp.ia.policy.PolicyEngine;
import com.erp.ia.policy.PolicyResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;

/**
 * Orchestrates the full agent lifecycle:
 * 1. Route intent → agent
 * 2. Agent.plan() → tool calls needed
 * 3. ContextAssembler executes tools, collects Evidence
 * 4. Agent.synthesize() → response + ActionPlan
 * 5. PolicyEngine validates ActionPlan
 * 6. DecisionLog records everything
 */
@Service
public class AgentOrchestrator {

    private static final Logger log = LoggerFactory.getLogger(AgentOrchestrator.class);

    private final AgentRegistry agentRegistry;
    private final ContextAssembler contextAssembler;
    private final PolicyEngine policyEngine;
    private final DecisionLogService decisionLogService;
    private final ObjectMapper objectMapper;

    public AgentOrchestrator(AgentRegistry agentRegistry,
            ContextAssembler contextAssembler,
            PolicyEngine policyEngine,
            DecisionLogService decisionLogService,
            ObjectMapper objectMapper) {
        this.agentRegistry = agentRegistry;
        this.contextAssembler = contextAssembler;
        this.policyEngine = policyEngine;
        this.decisionLogService = decisionLogService;
        this.objectMapper = objectMapper;
    }

    public AgentResponse process(AgentRequest request) {
        String correlationId = request.correlationId() != null
                ? request.correlationId()
                : MDC.get("correlationId");

        log.info("Processing intent: '{}' [correlation={}]", request.intent(), correlationId);

        // 1. Find agent
        AgentDefinition agent = agentRegistry.findByIntent(request.intent())
                .orElseThrow(() -> new IllegalArgumentException(
                        "No agent found for intent: " + request.intent()));

        log.info("Routed to agent: {}", agent.getName());

        // 2. Plan phase
        AgentPlan plan = agent.plan(request);
        log.info("Agent plan: {} tool calls", plan.toolCalls().size());

        // 3. Assemble context (execute tools, collect evidence)
        ContextSnapshot context = contextAssembler.assemble(request, agent.getName(), plan);
        log.info("Context assembled: {} evidences", context.getEvidences().size());

        // 4. Synthesize phase
        AgentResponse response = agent.synthesize(request, context);

        // 5. Validate action plan via PolicyEngine
        PolicyResult policyResult = PolicyResult.pass();
        if (response.actionPlan() != null && response.actionPlan().hasActions()) {
            policyResult = policyEngine.validate(response.actionPlan(), request);
            log.info("Policy result: {}", policyResult.status());
        }

        // 6. Log decision
        String actionPlanJson = serializeSafe(response.actionPlan());
        String inputDataJson = serializeSafe(request.context());
        String evidenceJson = serializeSafe(context.getEvidences());

        DecisionLog decisionLog = decisionLogService.logDecision(
                agent.getName(), request.intent(), correlationId,
                null, null, // prompt info filled by agent if needed
                inputDataJson, null, null,
                actionPlanJson, request.tenantId(), request.storeId());

        // Record tool calls in structured audit
        for (var evidence : context.getEvidences()) {
            DecisionToolCall toolCall = new DecisionToolCall(
                    evidence.source(), serializeSafe(evidence.query()),
                    serializeSafe(evidence.payload()), null);
            decisionLog.addToolCall(toolCall);
        }

        // Record policy results in structured audit
        for (var pr : policyResult.ruleResults()) {
            decisionLog.addPolicyResult(pr);
        }

        if (!policyResult.isPass()) {
            decisionLog.setStatus(DecisionLog.DecisionStatus.REJECTED);
        }

        // Save with children
        decisionLogService.findById(decisionLog.getId()); // trigger save via managed entity

        log.info("Decision logged: {} [status={}]", decisionLog.getId(), decisionLog.getStatus());

        // Return response with audit ID
        return new AgentResponse(
                response.response(),
                response.actionPlan(),
                response.evidence(),
                decisionLog.getId());
    }

    private String serializeSafe(Object obj) {
        if (obj == null)
            return null;
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            return obj.toString();
        }
    }
}
