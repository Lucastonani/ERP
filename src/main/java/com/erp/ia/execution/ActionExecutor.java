package com.erp.ia.execution;

import com.erp.ia.agent.model.ActionPlan;
import com.erp.ia.agent.model.PlannedAction;
import com.erp.ia.audit.DecisionLogService;
import com.erp.ia.audit.model.DecisionLog;
import com.erp.ia.execution.model.ExecutedAction;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * Execution Engine — executes approved ActionPlans with idempotency guarantee.
 * Routes ActionType → handler. Wraps everything in a transaction.
 */
@Service
public class ActionExecutor {

    private static final Logger log = LoggerFactory.getLogger(ActionExecutor.class);

    private final ExecutedActionRepository executedActionRepository;
    private final DecisionLogService decisionLogService;
    private final ObjectMapper objectMapper;

    public ActionExecutor(ExecutedActionRepository executedActionRepository,
            DecisionLogService decisionLogService,
            ObjectMapper objectMapper) {
        this.executedActionRepository = executedActionRepository;
        this.decisionLogService = decisionLogService;
        this.objectMapper = objectMapper;
    }

    /**
     * Execute all actions in an approved decision's ActionPlan.
     * Each action checks idempotency key before execution.
     */
    @Transactional
    public List<ExecutionResult> execute(String auditId, String executedBy) {
        DecisionLog decision = decisionLogService.findById(auditId)
                .orElseThrow(() -> new IllegalArgumentException("Decision not found: " + auditId));

        if (decision.getStatus() != DecisionLog.DecisionStatus.APPROVED) {
            throw new IllegalStateException(
                    "Decision must be APPROVED before execution. Current: " + decision.getStatus());
        }

        ActionPlan plan;
        try {
            plan = objectMapper.readValue(decision.getActionPlan(), ActionPlan.class);
        } catch (Exception e) {
            throw new RuntimeException("Failed to deserialize ActionPlan for decision: " + auditId, e);
        }

        List<ExecutionResult> results = new ArrayList<>();

        for (PlannedAction action : plan.actions()) {
            // Idempotency check
            if (executedActionRepository.existsByIdempotencyKey(action.getIdempotencyKey())) {
                log.info("Action already executed (idempotent): {}", action.getIdempotencyKey());
                ExecutedAction existing = executedActionRepository.findByIdempotencyKey(action.getIdempotencyKey())
                        .orElseThrow();
                results.add(new ExecutionResult(existing.getStatus(), existing.getActionType(),
                        "Already executed (idempotent)", null));
                continue;
            }

            ExecutionResult result;
            try {
                // In a full implementation, this would route to specific ActionHandlers
                log.info("Executing action: {} [key={}]", action.getType(), action.getIdempotencyKey());
                result = ExecutionResult.success(action.getType().name(),
                        "Action " + action.getType() + " executed successfully", action.getParams());
            } catch (Exception e) {
                result = ExecutionResult.failed(action.getType().name(), e.getMessage());
            }

            // Record execution
            ExecutedAction record = new ExecutedAction();
            record.setIdempotencyKey(action.getIdempotencyKey());
            record.setAuditId(auditId);
            record.setActionType(action.getType().name());
            record.setStatus(result.status());
            record.setExecutedBy(executedBy);
            try {
                record.setResultJson(objectMapper.writeValueAsString(result));
            } catch (Exception e) {
                record.setResultJson(result.toString());
            }
            executedActionRepository.save(record);

            results.add(result);
        }

        // Update decision status
        boolean allSuccess = results.stream().allMatch(ExecutionResult::isSuccess);
        decisionLogService.updateStatus(auditId,
                allSuccess ? DecisionLog.DecisionStatus.EXECUTED : DecisionLog.DecisionStatus.REJECTED);

        return results;
    }
}
