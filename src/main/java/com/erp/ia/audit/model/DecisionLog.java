package com.erp.ia.audit.model;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "decision_logs")
public class DecisionLog {

    public enum DecisionStatus {
        SUGGESTED, APPROVED, EXECUTED, REJECTED, OUTPUT_INVALID
    }

    @Id
    @Column(length = 36)
    private String id;

    @Column(name = "correlation_id", length = 36)
    private String correlationId;

    @Column(name = "agent_name", nullable = false, length = 100)
    private String agentName;

    @Column(nullable = false, length = 500)
    private String intent;

    @Column(name = "prompt_name", length = 100)
    private String promptName;

    @Column(name = "prompt_version")
    private Integer promptVersion;

    @Column(name = "input_data", columnDefinition = "TEXT")
    private String inputData;

    @Column(name = "llm_request", columnDefinition = "TEXT")
    private String llmRequest;

    @Column(name = "llm_response", columnDefinition = "TEXT")
    private String llmResponse;

    @Column(name = "action_plan", columnDefinition = "TEXT")
    private String actionPlan;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private DecisionStatus status = DecisionStatus.SUGGESTED;

    @Column(name = "approved_by", length = 100)
    private String approvedBy;

    @Column(name = "approved_at")
    private Instant approvedAt;

    @Column(name = "tenant_id", nullable = false, length = 50)
    private String tenantId = "default";

    @Column(name = "store_id", length = 50)
    private String storeId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @OneToMany(mappedBy = "decisionLog", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<DecisionToolCall> toolCalls = new ArrayList<>();

    @OneToMany(mappedBy = "decisionLog", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<DecisionPolicyResult> policyResults = new ArrayList<>();

    public DecisionLog() {
    }

    public void addToolCall(DecisionToolCall toolCall) {
        toolCalls.add(toolCall);
        toolCall.setDecisionLog(this);
    }

    public void addPolicyResult(DecisionPolicyResult result) {
        policyResults.add(result);
        result.setDecisionLog(this);
    }

    // --- Getters & Setters ---

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getCorrelationId() {
        return correlationId;
    }

    public void setCorrelationId(String correlationId) {
        this.correlationId = correlationId;
    }

    public String getAgentName() {
        return agentName;
    }

    public void setAgentName(String agentName) {
        this.agentName = agentName;
    }

    public String getIntent() {
        return intent;
    }

    public void setIntent(String intent) {
        this.intent = intent;
    }

    public String getPromptName() {
        return promptName;
    }

    public void setPromptName(String promptName) {
        this.promptName = promptName;
    }

    public Integer getPromptVersion() {
        return promptVersion;
    }

    public void setPromptVersion(Integer promptVersion) {
        this.promptVersion = promptVersion;
    }

    public String getInputData() {
        return inputData;
    }

    public void setInputData(String inputData) {
        this.inputData = inputData;
    }

    public String getLlmRequest() {
        return llmRequest;
    }

    public void setLlmRequest(String llmRequest) {
        this.llmRequest = llmRequest;
    }

    public String getLlmResponse() {
        return llmResponse;
    }

    public void setLlmResponse(String llmResponse) {
        this.llmResponse = llmResponse;
    }

    public String getActionPlan() {
        return actionPlan;
    }

    public void setActionPlan(String actionPlan) {
        this.actionPlan = actionPlan;
    }

    public DecisionStatus getStatus() {
        return status;
    }

    public void setStatus(DecisionStatus status) {
        this.status = status;
    }

    public String getApprovedBy() {
        return approvedBy;
    }

    public void setApprovedBy(String approvedBy) {
        this.approvedBy = approvedBy;
    }

    public Instant getApprovedAt() {
        return approvedAt;
    }

    public void setApprovedAt(Instant approvedAt) {
        this.approvedAt = approvedAt;
    }

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public String getStoreId() {
        return storeId;
    }

    public void setStoreId(String storeId) {
        this.storeId = storeId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public List<DecisionToolCall> getToolCalls() {
        return toolCalls;
    }

    public List<DecisionPolicyResult> getPolicyResults() {
        return policyResults;
    }
}
