package com.erp.ia.execution.model;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "executed_actions")
public class ExecutedAction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "idempotency_key", nullable = false, unique = true, length = 36)
    private String idempotencyKey;

    @Column(name = "audit_id", nullable = false, length = 36)
    private String auditId;

    @Column(name = "action_type", nullable = false, length = 50)
    private String actionType;

    @Column(nullable = false, length = 20)
    private String status = "SUCCESS";

    @Column(name = "result_json", columnDefinition = "TEXT")
    private String resultJson;

    @Column(name = "executed_by", length = 100)
    private String executedBy;

    @Column(name = "tenant_id", nullable = false, length = 50)
    private String tenantId = "default";

    @Column(name = "executed_at", nullable = false)
    private Instant executedAt = Instant.now();

    public ExecutedAction() {
    }

    // --- Getters & Setters ---
    public Long getId() {
        return id;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public void setIdempotencyKey(String key) {
        this.idempotencyKey = key;
    }

    public String getAuditId() {
        return auditId;
    }

    public void setAuditId(String auditId) {
        this.auditId = auditId;
    }

    public String getActionType() {
        return actionType;
    }

    public void setActionType(String actionType) {
        this.actionType = actionType;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getResultJson() {
        return resultJson;
    }

    public void setResultJson(String resultJson) {
        this.resultJson = resultJson;
    }

    public String getExecutedBy() {
        return executedBy;
    }

    public void setExecutedBy(String executedBy) {
        this.executedBy = executedBy;
    }

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public Instant getExecutedAt() {
        return executedAt;
    }
}
