package com.erp.ia.agent.model;

import java.util.List;

/**
 * A structured plan of actions proposed by an agent.
 */
public record ActionPlan(
        String summary, // human-readable summary
        List<PlannedAction> actions) {
    public static ActionPlan empty(String summary) {
        return new ActionPlan(summary, List.of());
    }

    public boolean hasActions() {
        return actions != null && !actions.isEmpty();
    }

    public boolean requiresApproval() {
        return actions != null && actions.stream().anyMatch(PlannedAction::isRequiresApproval);
    }
}
