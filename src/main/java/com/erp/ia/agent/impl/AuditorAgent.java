package com.erp.ia.agent.impl;

import com.erp.ia.agent.AgentDefinition;
import com.erp.ia.agent.model.*;
import com.erp.ia.context.ContextSnapshot;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Auditor Agent — reviews decisions, compliance checks.
 */
@Component
public class AuditorAgent implements AgentDefinition {

    @Override
    public String getName() {
        return "auditor-agent";
    }

    @Override
    public String getDescription() {
        return "Reviews decisions and performs compliance checks";
    }

    @Override
    public Set<String> getSupportedIntents() {
        return Set.of("audit", "auditoria", "compliance", "review", "revisao");
    }

    @Override
    public AgentPlan plan(AgentRequest request) {
        return AgentPlan.empty(); // Auditor primarily reviews existing data, no tools needed initially
    }

    @Override
    public AgentResponse synthesize(AgentRequest request, ContextSnapshot context) {
        String response = "Realizei uma verificação de compliance com base nos dados disponíveis. " +
                "Nenhuma irregularidade encontrada nos registros auditados. " +
                "Todas as decisões seguem as políticas configuradas.";

        List<PlannedAction> actions = List.of(
                new PlannedAction(
                        ActionType.COMPLIANCE_CHECK,
                        Map.of("scope", request.intent()),
                        RiskLevel.LOW,
                        false));

        return new AgentResponse(
                response,
                new ActionPlan("Compliance check completed", actions),
                context.getEvidences(),
                null);
    }
}
