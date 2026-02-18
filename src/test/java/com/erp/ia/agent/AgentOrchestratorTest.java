package com.erp.ia.agent;

import com.erp.ia.agent.model.*;
import com.erp.ia.audit.DecisionLogService;
import com.erp.ia.audit.model.DecisionLog;
import com.erp.ia.context.ContextAssembler;
import com.erp.ia.context.ContextSnapshot;
import com.erp.ia.context.Evidence;
import com.erp.ia.llm.LlmOutputValidator;
import com.erp.ia.llm.LlmPort;
import com.erp.ia.llm.LlmPort.LlmResponse;
import com.erp.ia.policy.PolicyEngine;
import com.erp.ia.prompt.PromptRegistryService;
import com.erp.ia.prompt.model.PromptTemplate;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AgentOrchestratorTest {

    @Mock
    private AgentRegistry agentRegistry;
    @Mock
    private ContextAssembler contextAssembler;
    @Mock
    private PolicyEngine policyEngine;
    @Mock
    private DecisionLogService decisionLogService;
    @Mock
    private LlmPort llmPort;
    @Mock
    private PromptRegistryService promptRegistryService;
    @Mock
    private LlmOutputValidator llmOutputValidator;
    @Mock
    private AgentDefinition mockAgent;

    private AgentOrchestrator orchestrator;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        orchestrator = new AgentOrchestrator(
                agentRegistry, contextAssembler, policyEngine, decisionLogService,
                objectMapper, llmPort, promptRegistryService, llmOutputValidator);
    }

    // ── Deterministic fallback tests ──

    @Test
    void shouldRouteIntentToCorrectAgent() {
        AgentRequest request = new AgentRequest("stock", Map.of(), "default", "default", "corr-1", "user1");

        when(agentRegistry.findByIntent("stock")).thenReturn(Optional.of(mockAgent));
        when(mockAgent.getName()).thenReturn("inventory-agent");
        when(mockAgent.usesLlm()).thenReturn(true);
        when(mockAgent.plan(any())).thenReturn(AgentPlan.empty());
        when(contextAssembler.assemble(any(), any(), any()))
                .thenReturn(new ContextSnapshot("inventory-agent", "stock", List.of()));
        // No prompt found → triggers deterministic fallback
        when(promptRegistryService.getActivePrompt("inventory-agent", "default"))
                .thenReturn(Optional.empty());
        when(mockAgent.synthesize(any(), any()))
                .thenReturn(new AgentResponse("OK", null, List.of(), null));

        DecisionLog log = new DecisionLog();
        log.setId("test-id");
        log.setAgentName("inventory-agent");
        log.setIntent("stock");
        when(decisionLogService.logDecision(any(), any(), any(), any(), any(), any(), any(), any(), any(), any(),
                any()))
                .thenReturn(log);

        AgentResponse response = orchestrator.process(request);

        assertNotNull(response);
        assertEquals("test-id", response.auditId());
        verify(agentRegistry).findByIntent("stock");
    }

    @Test
    void shouldThrowForUnknownIntent() {
        AgentRequest request = new AgentRequest("xyz", Map.of(), "default", "default", null, null);
        when(agentRegistry.findByIntent("xyz")).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> orchestrator.process(request));
    }

    // ── LLM-backed synthesis tests ──

    @Test
    void shouldUseLlmWhenPromptAvailable() {
        AgentRequest request = new AgentRequest("stock", Map.of(), "default", "default", "corr-2", "user1");

        when(agentRegistry.findByIntent("stock")).thenReturn(Optional.of(mockAgent));
        when(mockAgent.getName()).thenReturn("inventory-agent");
        when(mockAgent.usesLlm()).thenReturn(true);
        when(mockAgent.plan(any())).thenReturn(AgentPlan.empty());
        when(contextAssembler.assemble(any(), any(), any()))
                .thenReturn(new ContextSnapshot("inventory-agent", "stock", List.of()));

        // Prompt found
        PromptTemplate prompt = new PromptTemplate("inventory-agent", 1, "System prompt with {{evidence}}");
        when(promptRegistryService.getActivePrompt("inventory-agent", "default"))
                .thenReturn(Optional.of(prompt));

        // LLM returns valid response
        String llmJson = """
                {"response": "Estoque analisado", "reasoning": "dados ok", "actionPlan": {"summary": "Nada a fazer", "actions": []}}
                """;
        when(llmPort.complete(any())).thenReturn(
                new LlmResponse(llmJson, "gpt-4", "stop",
                        new LlmResponse.Usage(100, 50, 150), false, null));

        // Validator parses successfully
        LlmAgentOutput llmOutput = new LlmAgentOutput(
                "Estoque analisado", "dados ok",
                new LlmAgentOutput.LlmActionPlan("Nada a fazer", List.of()));
        when(llmOutputValidator.validateAndParse(eq(llmJson), eq(LlmAgentOutput.class)))
                .thenReturn(llmOutput);

        DecisionLog log = new DecisionLog();
        log.setId("llm-test-id");
        log.setAgentName("inventory-agent");
        log.setIntent("stock");
        when(decisionLogService.logDecision(
                eq("inventory-agent"), eq("stock"), eq("corr-2"),
                eq("inventory-agent"), eq(1),
                any(), any(), eq(llmJson),
                any(), eq("default"), eq("default")))
                .thenReturn(log);

        AgentResponse response = orchestrator.process(request);

        assertNotNull(response);
        assertEquals("Estoque analisado", response.response());
        assertEquals("llm-test-id", response.auditId());

        // Verify LLM was called
        verify(llmPort).complete(any());
        // Verify deterministic synthesize was NOT called
        verify(mockAgent, never()).synthesize(any(), any());
    }

    @Test
    void shouldFallbackToDeterministicWhenLlmFails() {
        AgentRequest request = new AgentRequest("stock", Map.of(), "default", "default", "corr-3", "user1");

        when(agentRegistry.findByIntent("stock")).thenReturn(Optional.of(mockAgent));
        when(mockAgent.getName()).thenReturn("inventory-agent");
        when(mockAgent.usesLlm()).thenReturn(true);
        when(mockAgent.plan(any())).thenReturn(AgentPlan.empty());
        when(contextAssembler.assemble(any(), any(), any()))
                .thenReturn(new ContextSnapshot("inventory-agent", "stock", List.of()));

        // Prompt found
        PromptTemplate prompt = new PromptTemplate("inventory-agent", 1, "System prompt {{evidence}}");
        when(promptRegistryService.getActivePrompt("inventory-agent", "default"))
                .thenReturn(Optional.of(prompt));

        // LLM returns error
        when(llmPort.complete(any())).thenReturn(
                LlmResponse.ofError("Service temporarily unavailable"));

        // Deterministic fallback
        when(mockAgent.synthesize(any(), any()))
                .thenReturn(new AgentResponse("Fallback response", null, List.of(), null));

        DecisionLog log = new DecisionLog();
        log.setId("fallback-id");
        log.setAgentName("inventory-agent");
        log.setIntent("stock");
        when(decisionLogService.logDecision(any(), any(), any(), any(), any(), any(), any(), any(), any(), any(),
                any()))
                .thenReturn(log);

        AgentResponse response = orchestrator.process(request);

        assertNotNull(response);
        assertEquals("Fallback response", response.response());
        // Verify deterministic synthesize WAS called as fallback
        verify(mockAgent).synthesize(any(), any());
    }

    @Test
    void shouldMarkOutputInvalidWhenLlmReturnsGarbage() {
        AgentRequest request = new AgentRequest("stock", Map.of(), "default", "default", "corr-4", "user1");

        when(agentRegistry.findByIntent("stock")).thenReturn(Optional.of(mockAgent));
        when(mockAgent.getName()).thenReturn("inventory-agent");
        when(mockAgent.usesLlm()).thenReturn(true);
        when(mockAgent.plan(any())).thenReturn(AgentPlan.empty());
        when(contextAssembler.assemble(any(), any(), any()))
                .thenReturn(new ContextSnapshot("inventory-agent", "stock", List.of()));

        // Prompt found
        PromptTemplate prompt = new PromptTemplate("inventory-agent", 1, "System prompt {{evidence}}");
        when(promptRegistryService.getActivePrompt("inventory-agent", "default"))
                .thenReturn(Optional.of(prompt));

        // LLM returns non-error but garbage content
        when(llmPort.complete(any())).thenReturn(
                new LlmResponse("This is not JSON at all", "gpt-4", "stop",
                        new LlmResponse.Usage(50, 20, 70), false, null));

        // Validator fails
        when(llmOutputValidator.validateAndParse(any(), eq(LlmAgentOutput.class)))
                .thenReturn(null);

        // Deterministic fallback
        when(mockAgent.synthesize(any(), any()))
                .thenReturn(new AgentResponse("Deterministic response", null, List.of(), null));

        DecisionLog log = new DecisionLog();
        log.setId("invalid-id");
        log.setAgentName("inventory-agent");
        log.setIntent("stock");
        when(decisionLogService.logDecision(any(), any(), any(), any(), any(), any(), any(), any(), any(), any(),
                any()))
                .thenReturn(log);

        AgentResponse response = orchestrator.process(request);

        assertNotNull(response);
        assertEquals("Deterministic response", response.response());
        // Verify status was set to OUTPUT_INVALID
        verify(decisionLogService).save(argThat(dl -> dl.getStatus() == DecisionLog.DecisionStatus.OUTPUT_INVALID));
    }

    @Test
    void shouldSkipLlmWhenAgentOptOut() {
        AgentRequest request = new AgentRequest("stock", Map.of(), "default", "default", "corr-5", "user1");

        when(agentRegistry.findByIntent("stock")).thenReturn(Optional.of(mockAgent));
        when(mockAgent.getName()).thenReturn("inventory-agent");
        when(mockAgent.usesLlm()).thenReturn(false); // Agent opts out
        when(mockAgent.plan(any())).thenReturn(AgentPlan.empty());
        when(contextAssembler.assemble(any(), any(), any()))
                .thenReturn(new ContextSnapshot("inventory-agent", "stock", List.of()));
        when(mockAgent.synthesize(any(), any()))
                .thenReturn(new AgentResponse("Direct response", null, List.of(), null));

        DecisionLog log = new DecisionLog();
        log.setId("no-llm-id");
        log.setAgentName("inventory-agent");
        log.setIntent("stock");
        when(decisionLogService.logDecision(any(), any(), any(), any(), any(), any(), any(), any(), any(), any(),
                any()))
                .thenReturn(log);

        AgentResponse response = orchestrator.process(request);

        assertNotNull(response);
        assertEquals("Direct response", response.response());
        // Verify LLM was NEVER called
        verify(llmPort, never()).complete(any());
        verify(promptRegistryService, never()).getActivePrompt(any(), any());
    }
}
