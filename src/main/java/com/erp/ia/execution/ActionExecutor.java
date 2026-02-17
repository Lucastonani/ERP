package com.erp.ia.execution;

import com.erp.ia.agent.model.ActionPlan;
import com.erp.ia.agent.model.ActionType;
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
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Execution Engine — executes approved ActionPlans with idempotency guarantee.
 * Routes ActionType → ActionHandler dynamically. Wraps everything in a
 * transaction.
 */
@Service
public class ActionExecutor {

    private static final Logger log = LoggerFactory.getLogger(ActionExecutor.class);

    private final ExecutedActionRepository executedActionRepository;
    private final DecisionLogService decisionLogService;
    private final ObjectMapper objectMapper;
    private final Map<ActionType, ActionHandler> handlerMap;

    public ActionExecutor(ExecutedActionRepository executedActionRepository,
            DecisionLogService decisionLogService,
            ObjectMapper objectMapper,
            List<ActionHandler> handlers) {
        this.executedActionRepository = executedActionRepository;
        this.decisionLogService = decisionLogService;
        this.objectMapper = objectMapper;
        this.handlerMap = new EnumMap<>(ActionType.class);
        handlers.forEach(h -> {
            ActionHandler previous = handlerMap.put(h.getActionType(), h);
            if (previous != null) {
                log.warn("Duplicate ActionHandler for {}: {} replaces {}",
                        h.getActionType(), h.getClass().getSimpleName(), previous.getClass().getSimpleName());
            }
            log.info("Registered ActionHandler: {} → {}", h.getActionType(), h.getClass().getSimpleName());
        });
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

            // Dispatch to registered handler
            ExecutionResult result;
            ActionHandler handler = handlerMap.get(action.getType());
            if (handler != null) {
                log.info("Dispatching action {} [key={}] to {}",
                        action.getType(), action.getIdempotencyKey(), handler.getClass().getSimpleName());
                result = handler.handle(action, auditId, executedBy);
            } else {
                log.warn("No ActionHandler registered for type: {}", action.getType());
                result = ExecutionResult.failed(action.getType().name(),
                        "No handler registered for action type: " + action.getType());
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
