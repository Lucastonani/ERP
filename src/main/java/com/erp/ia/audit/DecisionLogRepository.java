package com.erp.ia.audit;

import com.erp.ia.audit.model.DecisionLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DecisionLogRepository extends JpaRepository<DecisionLog, String> {
    Page<DecisionLog> findByAgentNameOrderByCreatedAtDesc(String agentName, Pageable pageable);

    Page<DecisionLog> findByCorrelationId(String correlationId, Pageable pageable);

    Page<DecisionLog> findByTenantIdOrderByCreatedAtDesc(String tenantId, Pageable pageable);
}
