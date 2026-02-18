package com.erp.ia.audit.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "decision_policy_results")
public class DecisionPolicyResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "decision_log_id", nullable = false)
    private DecisionLog decisionLog;

    @Column(name = "rule_name", nullable = false, length = 100)
    private String ruleName;

    @Column(nullable = false, length = 20)
    private String result; // PASS or BLOCKED

    @Column(length = 500)
    private String reason;

    @Column(name = "evaluated_at", nullable = false)
    private Instant evaluatedAt = Instant.now();

    public DecisionPolicyResult() {
    }

    public DecisionPolicyResult(String ruleName, String result, String reason) {
        this.ruleName = ruleName;
        this.result = result;
        this.reason = reason;
    }

    // --- Getters & Setters ---

    public Long getId() {
        return id;
    }

    public String getRuleName() {
        return ruleName;
    }

    public void setRuleName(String ruleName) {
        this.ruleName = ruleName;
    }

    public String getResult() {
        return result;
    }

    public void setResult(String result) {
        this.result = result;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public Instant getEvaluatedAt() {
        return evaluatedAt;
    }

    public DecisionLog getDecisionLog() {
        return decisionLog;
    }

    public void setDecisionLog(DecisionLog decisionLog) {
        this.decisionLog = decisionLog;
    }
}
