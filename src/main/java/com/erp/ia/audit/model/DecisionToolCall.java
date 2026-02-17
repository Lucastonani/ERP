package com.erp.ia.audit.model;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "decision_tool_calls")
public class DecisionToolCall {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "decision_log_id", nullable = false)
    private DecisionLog decisionLog;

    @Column(name = "tool_name", nullable = false, length = 100)
    private String toolName;

    @Column(name = "input_json", columnDefinition = "TEXT")
    private String inputJson;

    @Column(name = "output_json", columnDefinition = "TEXT")
    private String outputJson;

    @Column(name = "duration_ms")
    private Long durationMs;

    @Column(name = "called_at", nullable = false)
    private Instant calledAt = Instant.now();

    public DecisionToolCall() {
    }

    public DecisionToolCall(String toolName, String inputJson, String outputJson, Long durationMs) {
        this.toolName = toolName;
        this.inputJson = inputJson;
        this.outputJson = outputJson;
        this.durationMs = durationMs;
    }

    // --- Getters & Setters ---

    public Long getId() {
        return id;
    }

    public String getToolName() {
        return toolName;
    }

    public void setToolName(String toolName) {
        this.toolName = toolName;
    }

    public String getInputJson() {
        return inputJson;
    }

    public void setInputJson(String inputJson) {
        this.inputJson = inputJson;
    }

    public String getOutputJson() {
        return outputJson;
    }

    public void setOutputJson(String outputJson) {
        this.outputJson = outputJson;
    }

    public Long getDurationMs() {
        return durationMs;
    }

    public void setDurationMs(Long durationMs) {
        this.durationMs = durationMs;
    }

    public Instant getCalledAt() {
        return calledAt;
    }

    public DecisionLog getDecisionLog() {
        return decisionLog;
    }

    public void setDecisionLog(DecisionLog decisionLog) {
        this.decisionLog = decisionLog;
    }
}
