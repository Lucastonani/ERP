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
import com.erp.ia.execution.ExecutionResult;
import com.erp.ia.prompt.PromptRegistryService;
import com.erp.ia.prompt.model.PromptTemplate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test using a REAL PostgreSQL container via Testcontainers.
 *
 * Validates:
 * - Flyway migrations run on real Postgres (V1–V5)
 * - Full agent lifecycle (suggest → approve → execute) with real DB
 * - Audit trail persisted with children (tool calls, policy results)
 * - Prompt seeds from V5 migration are present
 * - Idempotency enforcement in ActionExecutor
 *
 * Requires Docker to be running. Skips automatically if Docker is unavailable.
 */
@SpringBootTest
@Testcontainers
@EnabledIf("isDockerAvailable")
class PostgresIntegrationTest {

    static boolean isDockerAvailable() {
        try {
            DockerClientFactory.instance().client();
            return true;
        } catch (Throwable t) {
            return false;
        }
    }

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("erp_test")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
        registry.add("spring.flyway.enabled", () -> "true");
        registry.add("security.enabled", () -> "false");
        registry.add("llm.provider", () -> "cloud");
        registry.add("llm.cloud.base-url", () -> "http://localhost:9999");
        registry.add("llm.cloud.api-key", () -> "test-key");
        registry.add("llm.cloud.model", () -> "test-model");
        registry.add("llm.timeout-connect", () -> "2s");
        registry.add("llm.timeout-read", () -> "5s");
        registry.add("llm.retry-max", () -> "0");
        registry.add("llm.circuit-breaker-threshold", () -> "3");
    }

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
    @Autowired
    private PromptRegistryService promptRegistryService;

    @BeforeEach
    void seedData() {
        stockRepository.deleteAll();
        productRepository.deleteAll();

        Product product = new Product();
        product.setSku("SKU-PG-001");
        product.setName("Widget Postgres");
        product.setCategory("widgets");
        product.setUnit("un");
        product.setTenantId("default");
        product = productRepository.save(product);

        Stock stock = new Stock();
        stock.setProduct(product);
        stock.setWarehouse("WH-01");
        stock.setQuantity(BigDecimal.valueOf(2));
        stock.setMinQuantity(BigDecimal.TEN);
        stock.setTenantId("default");
        stockRepository.save(stock);
    }

    // ────────────────────────────────────────────────────────────────
    // 1. Full round-trip: suggest → approve → execute
    // ────────────────────────────────────────────────────────────────

    @Test
    void fullLifecycleWithRealPostgres() {
        // 1. Suggest
        AgentRequest request = new AgentRequest(
                "reorder", Map.of(), "default", "default", "pg-corr-1", "admin");

        AgentResponse response = orchestrator.process(request);

        assertNotNull(response);
        assertNotNull(response.auditId());
        assertNotNull(response.response());
        assertNotNull(response.actionPlan());
        assertTrue(response.actionPlan().hasActions(), "ActionPlan should have actions");

        // 2. Verify audit was persisted with children
        Optional<DecisionLog> decision = decisionLogService.findById(response.auditId());
        assertTrue(decision.isPresent(), "DecisionLog must be persisted in Postgres");

        DecisionLog log = decision.get();
        assertEquals(DecisionLog.DecisionStatus.SUGGESTED, log.getStatus());
        assertEquals("inventory-agent", log.getAgentName());
        assertEquals("reorder", log.getIntent());
        assertEquals("pg-corr-1", log.getCorrelationId());
        assertNotNull(log.getCreatedAt(), "@PrePersist should have set createdAt");

        // Verify tool calls were persisted (children via @Transactional + save)
        assertFalse(log.getToolCalls().isEmpty(),
                "DecisionLog should have tool calls from ContextAssembler");

        // 3. Approve
        DecisionLog approved = decisionLogService.approve(response.auditId(), "cfo-user");
        assertEquals(DecisionLog.DecisionStatus.APPROVED, approved.getStatus());
        assertEquals("cfo-user", approved.getApprovedBy());
        assertNotNull(approved.getApprovedAt());

        // 4. Execute
        List<ExecutionResult> results = actionExecutor.execute(response.auditId(), "system");
        assertNotNull(results);
        assertFalse(results.isEmpty());
        assertTrue(results.stream().allMatch(ExecutionResult::isSuccess),
                "All actions should execute successfully");

        // Verify final status
        DecisionLog executed = decisionLogService.findById(response.auditId()).orElseThrow();
        assertEquals(DecisionLog.DecisionStatus.EXECUTED, executed.getStatus());
    }

    // ────────────────────────────────────────────────────────────────
    // 2. Verify prompt seeds from V5 migration
    // ────────────────────────────────────────────────────────────────

    @Test
    void promptSeedsShouldExistFromMigration() {
        Optional<PromptTemplate> inventoryPrompt = promptRegistryService.getActivePrompt("inventory-agent", "default");
        assertTrue(inventoryPrompt.isPresent(), "inventory-agent prompt should be seeded by V5");
        assertEquals(1, inventoryPrompt.get().getVersion());
        assertTrue(inventoryPrompt.get().getContent().contains("{{evidence}}"));

        Optional<PromptTemplate> purchasingPrompt = promptRegistryService.getActivePrompt("purchasing-agent",
                "default");
        assertTrue(purchasingPrompt.isPresent(), "purchasing-agent prompt should be seeded");

        Optional<PromptTemplate> auditorPrompt = promptRegistryService.getActivePrompt("auditor-agent", "default");
        assertTrue(auditorPrompt.isPresent(), "auditor-agent prompt should be seeded");
    }

    // ────────────────────────────────────────────────────────────────
    // 3. Idempotency: executing twice should not duplicate
    // ────────────────────────────────────────────────────────────────

    @Test
    void executionShouldBeIdempotent() {
        AgentRequest request = new AgentRequest(
                "reorder", Map.of(), "default", "default", "pg-corr-idem", "admin");
        AgentResponse response = orchestrator.process(request);
        decisionLogService.approve(response.auditId(), "admin");

        // First execution succeeds
        List<ExecutionResult> first = actionExecutor.execute(response.auditId(), "system");
        assertFalse(first.isEmpty());
        assertTrue(first.stream().allMatch(ExecutionResult::isSuccess));

        // Second execution should throw — decision is now EXECUTED, not APPROVED
        assertThrows(IllegalStateException.class, () -> actionExecutor.execute(response.auditId(), "system"),
                "Cannot execute an already-executed decision");
    }

    // ────────────────────────────────────────────────────────────────
    // 4. Stock query on real Postgres
    // ────────────────────────────────────────────────────────────────

    @Test
    void stockQueryWithRealPostgres() {
        AgentRequest request = new AgentRequest(
                "stock", Map.of(), "default", "default", "pg-corr-stock", "warehouse-user");

        AgentResponse response = orchestrator.process(request);

        assertNotNull(response);
        assertNotNull(response.auditId());
        assertFalse(response.evidence().isEmpty(), "Evidence should contain stock data");
    }

    // ────────────────────────────────────────────────────────────────
    // 5. Flyway migrations validated (implicit — context loads)
    // ────────────────────────────────────────────────────────────────

    @Test
    void flywayMigrationsRunSuccessfully() {
        // If we reach here, all 5 Flyway migrations ran on real Postgres without
        // errors.
        List<Product> products = productRepository.findByTenantId("default");
        assertFalse(products.isEmpty(), "Seeded products should exist");
    }
}
