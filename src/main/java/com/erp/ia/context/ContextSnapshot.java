package com.erp.ia.context;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Snapshot of all evidence gathered for an agent decision.
 * Immutable after assembly — used for audit and reproducibility.
 */
public class ContextSnapshot {

    private final String agentName;
    private final String intent;
    private final List<Evidence> evidences;
    private final Instant assembledAt;

    public ContextSnapshot(String agentName, String intent, List<Evidence> evidences) {
        this.agentName = agentName;
        this.intent = intent;
        this.evidences = Collections.unmodifiableList(new ArrayList<>(evidences));
        this.assembledAt = Instant.now();
    }

    public String getAgentName() {
        return agentName;
    }

    public String getIntent() {
        return intent;
    }

    public List<Evidence> getEvidences() {
        return evidences;
    }

    public Instant getAssembledAt() {
        return assembledAt;
    }

    public boolean hasEvidence() {
        return !evidences.isEmpty();
    }
}
