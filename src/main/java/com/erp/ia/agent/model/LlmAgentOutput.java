package com.erp.ia.agent.model;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.Map;

/**
 * DTO that represents the expected structured output from an LLM call.
 * Must match the JSON schema specified in the agent prompts (V5 migration).
 *
 * Expected LLM JSON format:
 * 
 * <pre>
 * {
 *   "response": "análise em linguagem natural",
 *   "reasoning": "explicação do raciocínio (opcional)",
 *   "actionPlan": {
 *     "summary": "resumo das ações",
 *     "actions": [
 *       { "type": "DRAFT_PURCHASE_ORDER", "params": {...}, "risk": "MEDIUM", "requiresApproval": true }
 *     ]
 *   }
 * }
 * </pre>
 *
 * Validated via Bean Validation by {@link com.erp.ia.llm.LlmOutputValidator}.
 * If validation fails → DecisionLog status = OUTPUT_INVALID, fallback to
 * deterministic synthesize().
 */
public record LlmAgentOutput(

        @NotBlank(message = "LLM response text is required") String response,

        String reasoning,

        @Valid LlmActionPlan actionPlan) {

    /**
     * The action plan portion of the LLM output.
     */
    public record LlmActionPlan(
            String summary,
            @Valid List<LlmPlannedAction> actions) {
    }

    /**
     * A single action proposed by the LLM.
     */
    public record LlmPlannedAction(

            @NotNull(message = "Action type is required") String type,

            @Schema(description = "Action parameters (arbitrary JSON)") Map<String, Object> params,

            String risk,

            boolean requiresApproval) {

        /**
         * Convert to the domain PlannedAction, validating enums.
         *
         * @throws IllegalArgumentException if type or risk are invalid enums
         */
        public PlannedAction toDomain() {
            ActionType actionType = ActionType.valueOf(type.toUpperCase());
            RiskLevel riskLevel = risk != null
                    ? RiskLevel.valueOf(risk.toUpperCase())
                    : RiskLevel.MEDIUM;
            return new PlannedAction(actionType, params, riskLevel, requiresApproval);
        }
    }

    /**
     * Convert this LLM output to a domain ActionPlan.
     * Invalid individual actions are silently skipped (logged elsewhere).
     */
    public ActionPlan toDomainActionPlan() {
        String summary = actionPlan != null && actionPlan.summary() != null
                ? actionPlan.summary()
                : response;

        if (actionPlan == null || actionPlan.actions() == null || actionPlan.actions().isEmpty()) {
            return ActionPlan.empty(summary);
        }

        List<PlannedAction> domainActions = actionPlan.actions().stream()
                .map(a -> {
                    try {
                        return a.toDomain();
                    } catch (IllegalArgumentException e) {
                        return null;
                    }
                })
                .filter(java.util.Objects::nonNull)
                .toList();

        return new ActionPlan(summary, domainActions);
    }
}
