package com.erp.ia.audit;

import com.erp.ia.audit.model.DecisionLog;
import com.erp.ia.audit.model.DecisionLog.DecisionStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
public class DecisionLogService {

    private final DecisionLogRepository repository;

    public DecisionLogService(DecisionLogRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public DecisionLog logDecision(String agentName, String intent, String correlationId,
            String promptName, Integer promptVersion,
            String inputData, String llmRequest, String llmResponse,
            String actionPlan, String tenantId, String storeId) {
        DecisionLog log = new DecisionLog();
        log.setId(UUID.randomUUID().toString());
        log.setCorrelationId(correlationId);
        log.setAgentName(agentName);
        log.setIntent(intent);
        log.setPromptName(promptName);
        log.setPromptVersion(promptVersion);
        log.setInputData(inputData);
        log.setLlmRequest(llmRequest);
        log.setLlmResponse(llmResponse);
        log.setActionPlan(actionPlan);
        log.setTenantId(tenantId);
        log.setStoreId(storeId);
        return repository.save(log);
    }

    @Transactional
    public DecisionLog save(DecisionLog decisionLog) {
        return repository.save(decisionLog);
    }

    public Optional<DecisionLog> findById(String id) {
        return repository.findById(id);
    }

    public Page<DecisionLog> findByAgent(String agentName, Pageable pageable) {
        return repository.findByAgentNameOrderByCreatedAtDesc(agentName, pageable);
    }

    public Page<DecisionLog> findAll(Pageable pageable) {
        return repository.findAll(pageable);
    }

    @Transactional
    public DecisionLog approve(String decisionId, String approvedBy) {
        DecisionLog log = repository.findById(decisionId)
                .orElseThrow(() -> new IllegalArgumentException("Decision not found: " + decisionId));
        log.setStatus(DecisionStatus.APPROVED);
        log.setApprovedBy(approvedBy);
        log.setApprovedAt(Instant.now());
        return repository.save(log);
    }

    @Transactional
    public DecisionLog updateStatus(String decisionId, DecisionStatus status) {
        DecisionLog log = repository.findById(decisionId)
                .orElseThrow(() -> new IllegalArgumentException("Decision not found: " + decisionId));
        log.setStatus(status);
        return repository.save(log);
    }
}
