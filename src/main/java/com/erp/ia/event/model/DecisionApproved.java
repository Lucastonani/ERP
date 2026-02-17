package com.erp.ia.event.model;

import com.erp.ia.event.DomainEvent;

public class DecisionApproved extends DomainEvent {

    private final String decisionId;
    private final String agentName;
    private final String approvedBy;

    public DecisionApproved(String decisionId, String agentName, String approvedBy) {
        super("DECISION_APPROVED", 1);
        this.decisionId = decisionId;
        this.agentName = agentName;
        this.approvedBy = approvedBy;
    }

    public String getDecisionId() {
        return decisionId;
    }

    public String getAgentName() {
        return agentName;
    }

    public String getApprovedBy() {
        return approvedBy;
    }
}
