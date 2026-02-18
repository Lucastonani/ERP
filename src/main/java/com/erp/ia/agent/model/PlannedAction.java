package com.erp.ia.agent.model;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Map;
import java.util.UUID;

/**
 * A single planned action within an ActionPlan.
 * Typed, with risk assessment, approval flag, and idempotency key.
 */
public class PlannedAction {

    private ActionType type;
    @Schema(description = "Action parameters (arbitrary key-value pairs)")
    private Map<String, Object> params;
    private RiskLevel risk;
    private boolean requiresApproval;
    private String idempotencyKey;

    public PlannedAction() {
        this.idempotencyKey = UUID.randomUUID().toString();
    }

    public PlannedAction(ActionType type, Map<String, Object> params, RiskLevel risk, boolean requiresApproval) {
        this.type = type;
        this.params = params;
        this.risk = risk;
        this.requiresApproval = requiresApproval;
        this.idempotencyKey = UUID.randomUUID().toString();
    }

    // --- Getters & Setters ---

    public ActionType getType() {
        return type;
    }

    public void setType(ActionType type) {
        this.type = type;
    }

    public Map<String, Object> getParams() {
        return params;
    }

    public void setParams(Map<String, Object> params) {
        this.params = params;
    }

    public RiskLevel getRisk() {
        return risk;
    }

    public void setRisk(RiskLevel risk) {
        this.risk = risk;
    }

    public boolean isRequiresApproval() {
        return requiresApproval;
    }

    public void setRequiresApproval(boolean requiresApproval) {
        this.requiresApproval = requiresApproval;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public void setIdempotencyKey(String idempotencyKey) {
        this.idempotencyKey = idempotencyKey;
    }
}
