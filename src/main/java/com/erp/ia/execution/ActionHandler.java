package com.erp.ia.execution;

import com.erp.ia.agent.model.ActionType;
import com.erp.ia.agent.model.PlannedAction;

/**
 * Contract for executing a specific ActionType.
 * Each handler is auto-discovered by Spring and registered by ActionType.
 * Handlers delegate to Core services — they never access JPA directly.
 */
public interface ActionHandler {

    /**
     * Which ActionType this handler processes.
     */
    ActionType getActionType();

    /**
     * Execute the planned action against the core.
     *
     * @param action     the planned action with typed params
     * @param auditId    the decision audit ID for traceability
     * @param executedBy who triggered execution
     * @return result with status, message, and optional output
     */
    ExecutionResult handle(PlannedAction action, String auditId, String executedBy);
}
