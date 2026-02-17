package com.erp.ia.context;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ContextSnapshotTest {

    @Test
    void shouldBeImmutable() {
        List<Evidence> evidences = new java.util.ArrayList<>();
        evidences.add(new Evidence("tool1", "query1", Map.of("data", "value")));

        ContextSnapshot snapshot = new ContextSnapshot("agent", "intent", evidences);

        // Modifying original list should not affect snapshot
        evidences.add(new Evidence("tool2", "query2", "data2"));
        assertEquals(1, snapshot.getEvidences().size());
    }

    @Test
    void shouldReportHasEvidence() {
        ContextSnapshot withEvidence = new ContextSnapshot("agent", "intent",
                List.of(new Evidence("tool1", "query1", "data1")));
        assertTrue(withEvidence.hasEvidence());

        ContextSnapshot noEvidence = new ContextSnapshot("agent", "intent", List.of());
        assertFalse(noEvidence.hasEvidence());
    }
}
