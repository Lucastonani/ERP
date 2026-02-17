package com.erp.ia.agent;

import com.erp.ia.agent.impl.AuditorAgent;
import com.erp.ia.agent.impl.InventoryAgent;
import com.erp.ia.agent.impl.PurchasingAgent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class AgentRegistryTest {

    private AgentRegistry registry;

    @BeforeEach
    void setUp() {
        registry = new AgentRegistry(List.of(
                new InventoryAgent(),
                new PurchasingAgent(),
                new AuditorAgent()));
    }

    @Test
    void shouldFindInventoryAgentByExactIntent() {
        Optional<AgentDefinition> agent = registry.findByIntent("stock");
        assertTrue(agent.isPresent());
        assertEquals("inventory-agent", agent.get().getName());
    }

    @Test
    void shouldFindPurchasingAgentByIntent() {
        Optional<AgentDefinition> agent = registry.findByIntent("compra");
        assertTrue(agent.isPresent());
        assertEquals("purchasing-agent", agent.get().getName());
    }

    @Test
    void shouldFindAuditorAgentByIntent() {
        Optional<AgentDefinition> agent = registry.findByIntent("audit");
        assertTrue(agent.isPresent());
        assertEquals("auditor-agent", agent.get().getName());
    }

    @Test
    void shouldReturnEmptyForUnknownIntent() {
        Optional<AgentDefinition> agent = registry.findByIntent("xyz_unknown_intent");
        assertTrue(agent.isEmpty());
    }

    @Test
    void shouldFindAgentByPartialMatch() {
        Optional<AgentDefinition> agent = registry.findByIntent("verificar estoque agora");
        assertTrue(agent.isPresent());
        assertEquals("inventory-agent", agent.get().getName());
    }

    @Test
    void shouldFindAgentByName() {
        Optional<AgentDefinition> agent = registry.findByName("auditor-agent");
        assertTrue(agent.isPresent());
        assertEquals("auditor-agent", agent.get().getName());
    }

    @Test
    void shouldListAllAgents() {
        assertEquals(3, registry.getAll().size());
    }
}
