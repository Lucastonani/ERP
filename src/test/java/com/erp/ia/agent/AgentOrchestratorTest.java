package com.erp.ia.agent;

import com.erp.ia.agent.model.AgentPlan;
import com.erp.ia.agent.model.AgentRequest;
import com.erp.ia.agent.model.AgentResponse;
import com.erp.ia.audit.DecisionLogService;
import com.erp.ia.audit.model.DecisionLog;
import com.erp.ia.context.ContextAssembler;
import com.erp.ia.context.ContextSnapshot;

import com.erp.ia.policy.PolicyEngine;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
    private AgentDefinition mockAgent;

    private AgentOrchestrator orchestrator;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        orchestrator = new AgentOrchestrator(
                agentRegistry, contextAssembler, policyEngine, decisionLogService, objectMapper);
    }

    @Test
    void shouldRouteIntentToCorrectAgent() {
        AgentRequest request = new AgentRequest("stock", Map.of(), "default", "default", "corr-1", "user1");

        when(agentRegistry.findByIntent("stock")).thenReturn(Optional.of(mockAgent));
        when(mockAgent.getName()).thenReturn("inventory-agent");
        when(mockAgent.plan(any())).thenReturn(AgentPlan.empty());
        when(contextAssembler.assemble(any(), any(), any()))
                .thenReturn(new ContextSnapshot("inventory-agent", "stock", List.of()));
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
}
