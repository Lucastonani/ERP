package com.erp.ia.integration;

import com.erp.ia.agent.AgentOrchestrator;
import com.erp.ia.agent.model.AgentRequest;
import com.erp.ia.agent.model.AgentResponse;
import com.erp.ia.audit.DecisionLogService;
import com.erp.ia.audit.model.DecisionLog;
import com.erp.ia.core.model.Product;
import com.erp.ia.core.model.Stock;
import com.erp.ia.core.repository.ProductRepository;
import com.erp.ia.core.repository.StockRepository;
import com.erp.ia.execution.ActionExecutor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class AgentLifecycleIntegrationTest {

    @Autowired
    private AgentOrchestrator orchestrator;
    @Autowired
    private DecisionLogService decisionLogService;
    @Autowired
    private ActionExecutor actionExecutor;
    @Autowired
    private ProductRepository productRepository;
    @Autowired
    private StockRepository stockRepository;

    @BeforeEach
    void seedData() {
        Product product = new Product();
        product.setSku("SKU-001");
        product.setName("Widget A");
        product.setCategory("widgets");
        product.setUnit("un");
        product.setTenantId("default");
        product = productRepository.save(product);

        Stock stock = new Stock();
        stock.setProduct(product);
        stock.setWarehouse("WH-01");
        stock.setQuantity(BigDecimal.valueOf(3));
        stock.setMinQuantity(BigDecimal.TEN);
        stock.setTenantId("default");
        stockRepository.save(stock);
    }

    @Test
    void fullLifecycleSuggestApproveExecute() {
        // 1. Suggest (via agent orchestrator)
        AgentRequest request = new AgentRequest(
                "reorder", Map.of(), "default", "default", "test-corr-1", "admin");

        AgentResponse response = orchestrator.process(request);

        assertNotNull(response);
        assertNotNull(response.auditId());
        assertNotNull(response.response());
        assertNotNull(response.actionPlan());
        assertTrue(response.actionPlan().hasActions());

        // 2. Verify decision was logged
        Optional<DecisionLog> decision = decisionLogService.findById(response.auditId());
        assertTrue(decision.isPresent());
        assertEquals(DecisionLog.DecisionStatus.SUGGESTED, decision.get().getStatus());
        assertEquals("inventory-agent", decision.get().getAgentName());

        // 3. Approve
        DecisionLog approved = decisionLogService.approve(response.auditId(), "admin-user");
        assertEquals(DecisionLog.DecisionStatus.APPROVED, approved.getStatus());
        assertNotNull(approved.getApprovedAt());
    }

    @Test
    void stockQueryShouldReturnData() {
        AgentRequest request = new AgentRequest(
                "stock", Map.of(), "default", "default", "test-corr-2", "user1");

        AgentResponse response = orchestrator.process(request);

        assertNotNull(response);
        assertNotNull(response.auditId());
        assertFalse(response.evidence().isEmpty());
    }

    @Test
    void complianceCheckShouldWork() {
        AgentRequest request = new AgentRequest(
                "audit", Map.of(), "default", "default", "test-corr-3", "auditor1");

        AgentResponse response = orchestrator.process(request);

        assertNotNull(response);
        assertNotNull(response.auditId());
    }
}
