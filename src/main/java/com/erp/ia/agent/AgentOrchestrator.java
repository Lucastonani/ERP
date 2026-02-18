package com.erp.ia.agent;

import com.erp.ia.agent.model.*;
import com.erp.ia.audit.DecisionLogService;
import com.erp.ia.audit.model.DecisionLog;
import com.erp.ia.audit.model.DecisionToolCall;
import com.erp.ia.context.ContextAssembler;
import com.erp.ia.context.ContextSnapshot;
import com.erp.ia.llm.LlmOutputValidator;
import com.erp.ia.llm.LlmPort;
import com.erp.ia.llm.LlmPort.LlmRequest;
import com.erp.ia.llm.LlmPort.LlmResponse;
import com.erp.ia.policy.PolicyEngine;
import com.erp.ia.policy.PolicyResult;
import com.erp.ia.prompt.PromptRegistryService;
import com.erp.ia.prompt.model.PromptTemplate;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Orchestrates the full agent lifecycle:
 * 1. Route intent → agent
 * 2. Agent.plan() → tool calls needed
 * 3. ContextAssembler executes tools, collects Evidence
 * 4. If agent.usesLlm():
 * a. Resolve prompt from PromptRegistryService
 * b. Call LlmPort with prompt + evidence
 * c. Validate output via LlmOutputValidator → LlmAgentOutput
 * d. If valid → use LLM response + ActionPlan
 * e. If invalid/error → fallback to agent.synthesize()
 * 5. PolicyEngine validates ActionPlan
 * 6. DecisionLog records everything (with children persisted in same TX)
 *
 * IMPORTANT: This method is @Transactional so the DecisionLog and its
 * children (tool calls, policy results) are all persisted atomically.
 */
@Service
public class AgentOrchestrator {

    private static final Logger log = LoggerFactory.getLogger(AgentOrchestrator.class);

    private final AgentRegistry agentRegistry;
    private final ContextAssembler contextAssembler;
    private final PolicyEngine policyEngine;
    private final DecisionLogService decisionLogService;
    private final ObjectMapper objectMapper;
    private final LlmPort llmPort;
    private final PromptRegistryService promptRegistryService;
    private final LlmOutputValidator llmOutputValidator;

    public AgentOrchestrator(AgentRegistry agentRegistry,
            ContextAssembler contextAssembler,
            PolicyEngine policyEngine,
            DecisionLogService decisionLogService,
            ObjectMapper objectMapper,
            LlmPort llmPort,
            PromptRegistryService promptRegistryService,
            LlmOutputValidator llmOutputValidator) {
        this.agentRegistry = agentRegistry;
        this.contextAssembler = contextAssembler;
        this.policyEngine = policyEngine;
        this.decisionLogService = decisionLogService;
        this.objectMapper = objectMapper;
        this.llmPort = llmPort;
        this.promptRegistryService = promptRegistryService;
        this.llmOutputValidator = llmOutputValidator;
    }

    @Transactional
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

        // 4. Synthesize: try LLM first, fallback to deterministic
        AgentResponse response;
        String promptName = null;
        Integer promptVersion = null;
        String llmRequestJson = null;
        String llmResponseJson = null;
        DecisionLog.DecisionStatus outputStatus = null;

        if (agent.usesLlm()) {
            // 4a. Try LLM-backed synthesis
            LlmSynthesisResult llmResult = synthesizeViaLlm(agent, request, context);
            promptName = llmResult.promptName;
            promptVersion = llmResult.promptVersion;
            llmRequestJson = llmResult.llmRequestJson;
            llmResponseJson = llmResult.llmResponseJson;

            if (llmResult.response != null) {
                response = llmResult.response;
                log.info("LLM synthesis succeeded for agent '{}'", agent.getName());
            } else if (llmResult.outputInvalid) {
                outputStatus = DecisionLog.DecisionStatus.OUTPUT_INVALID;
                response = agent.synthesize(request, context);
                log.warn("LLM output invalid — falling back to deterministic synthesize for '{}'",
                        agent.getName());
            } else {
                response = agent.synthesize(request, context);
                log.warn("LLM unavailable — falling back to deterministic synthesize for '{}'",
                        agent.getName());
            }
        } else {
            // 4b. Agent opts out of LLM
            response = agent.synthesize(request, context);
            log.info("Agent '{}' uses deterministic synthesize (usesLlm=false)", agent.getName());
        }

        // 5. Validate action plan via PolicyEngine
        PolicyResult policyResult = PolicyResult.pass();
        if (response.actionPlan() != null && response.actionPlan().hasActions()) {
            policyResult = policyEngine.validate(response.actionPlan(), request);
            log.info("Policy result: {}", policyResult.status());
        }

        // 6. Log decision — parent saved first
        String actionPlanJson = serializeSafe(response.actionPlan());
        String inputDataJson = serializeSafe(request.context());

        DecisionLog decisionLog = decisionLogService.logDecision(
                agent.getName(), request.intent(), correlationId,
                promptName, promptVersion,
                inputDataJson, llmRequestJson, llmResponseJson,
                actionPlanJson, request.tenantId(), request.storeId());

        // Override status if output was invalid
        if (outputStatus != null) {
            decisionLog.setStatus(outputStatus);
        }

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

        // If policy blocked, set REJECTED and strip ActionPlan from response
        if (!policyResult.isPass()) {
            decisionLog.setStatus(DecisionLog.DecisionStatus.REJECTED);
        }

        // Explicit save — guarantees children (toolCalls, policyResults) and status are
        // persisted
        decisionLogService.save(decisionLog);

        log.info("Decision logged: {} [status={}]", decisionLog.getId(), decisionLog.getStatus());

        // 7. Build safe response — if policy blocked, DO NOT leak ActionPlan to client
        ActionPlan safePlan;
        String safeMessage;

        if (policyResult.isPass()) {
            safePlan = response.actionPlan();
            safeMessage = response.response();
        } else {
            safePlan = ActionPlan.empty("Ação bloqueada por políticas");
            safeMessage = "Ação bloqueada por políticas: "
                    + String.join("; ", policyResult.reasons());
            log.warn("ActionPlan stripped from response — policy BLOCKED [decision={}]",
                    decisionLog.getId());
        }

        return new AgentResponse(
                safeMessage,
                safePlan,
                response.evidence(),
                decisionLog.getId());
    }

    // ────────────────────────────────────────────────────────────
    // LLM Synthesis
    // ────────────────────────────────────────────────────────────

    private LlmSynthesisResult synthesizeViaLlm(AgentDefinition agent,
            AgentRequest request, ContextSnapshot context) {
        LlmSynthesisResult result = new LlmSynthesisResult();

        // 1. Resolve prompt template
        Optional<PromptTemplate> promptOpt = promptRegistryService.getActivePrompt(
                agent.getName(), request.tenantId());

        if (promptOpt.isEmpty()) {
            log.warn("No active prompt found for agent '{}' / tenant '{}' — skipping LLM",
                    agent.getName(), request.tenantId());
            return result;
        }

        PromptTemplate prompt = promptOpt.get();
        result.promptName = prompt.getName();
        result.promptVersion = prompt.getVersion();

        // 2. Build prompt content (replace {{evidence}} with actual evidence)
        String evidenceJson = serializeSafe(context.getEvidences());
        String resolvedPrompt = prompt.getContent().replace("{{evidence}}", evidenceJson);

        // 3. Build LLM request
        LlmRequest llmRequest = new LlmRequest(
                null, // use default model from config
                List.of(
                        new LlmRequest.Message("system", resolvedPrompt),
                        new LlmRequest.Message("user",
                                "Intent: " + request.intent()
                                        + ". Responda em JSON conforme o formato especificado.")),
                0.3, // low temperature for structured output
                2048);

        result.llmRequestJson = serializeSafe(llmRequest);

        // 4. Call LLM
        LlmResponse llmResponse;
        try {
            llmResponse = llmPort.complete(llmRequest);
        } catch (Exception e) {
            log.error("LLM call failed for agent '{}': {}", agent.getName(), e.getMessage());
            return result;
        }

        if (llmResponse.error()) {
            log.warn("LLM returned error for agent '{}': {}", agent.getName(), llmResponse.errorMessage());
            result.llmResponseJson = serializeSafe(llmResponse);
            return result;
        }

        result.llmResponseJson = llmResponse.content();

        // 5. Validate and parse LLM output
        LlmAgentOutput llmOutput = llmOutputValidator.validateAndParse(
                llmResponse.content(), LlmAgentOutput.class);

        if (llmOutput == null) {
            log.warn("LLM output validation failed for agent '{}'", agent.getName());
            result.outputInvalid = true;
            return result;
        }

        // 6. Convert to domain objects
        ActionPlan actionPlan = llmOutput.toDomainActionPlan();

        result.response = new AgentResponse(
                llmOutput.response(),
                actionPlan,
                context.getEvidences(),
                null); // audit ID set later

        return result;
    }

    /**
     * Internal result holder for LLM synthesis attempt.
     */
    private static class LlmSynthesisResult {
        String promptName;
        Integer promptVersion;
        String llmRequestJson;
        String llmResponseJson;
        boolean outputInvalid;
        AgentResponse response; // null if LLM failed/unavailable
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
