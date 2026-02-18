package com.erp.ia.web;

import com.erp.ia.audit.DecisionLogService;
import com.erp.ia.audit.model.DecisionLog;
import com.erp.ia.event.EventBus;
import com.erp.ia.event.model.DecisionApproved;
import com.erp.ia.execution.ActionExecutor;
import com.erp.ia.execution.ExecutionResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/actions")
@Tag(name = "Actions", description = "Approve, execute, and query AI-suggested actions")
public class ActionController {

    private final DecisionLogService decisionLogService;
    private final ActionExecutor actionExecutor;
    private final EventBus eventBus;

    public ActionController(DecisionLogService decisionLogService,
            ActionExecutor actionExecutor,
            EventBus eventBus) {
        this.decisionLogService = decisionLogService;
        this.actionExecutor = actionExecutor;
        this.eventBus = eventBus;
    }

    @GetMapping("/{auditId}")
    @Operation(summary = "Get decision details", description = "Returns the full decision log for a given audit ID")
    public ResponseEntity<Map<String, Object>> getDecision(@PathVariable String auditId) {
        DecisionLog decision = decisionLogService.findById(auditId)
                .orElseThrow(() -> new IllegalArgumentException("Decision not found: " + auditId));

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("auditId", decision.getId());
        result.put("agentName", decision.getAgentName());
        result.put("intent", decision.getIntent());
        result.put("status", decision.getStatus().name());
        result.put("tenantId", decision.getTenantId());
        result.put("createdAt", decision.getCreatedAt().toString());
        if (decision.getApprovedBy() != null) {
            result.put("approvedBy", decision.getApprovedBy());
            result.put("approvedAt", decision.getApprovedAt().toString());
        }
        result.put("toolCalls", decision.getToolCalls().size());
        result.put("policyResults", decision.getPolicyResults().size());
        return ResponseEntity.ok(result);
    }

    @GetMapping
    @Operation(summary = "List recent decisions", description = "Returns paginated list of decisions, optionally filtered by agent name")
    public ResponseEntity<Map<String, Object>> listDecisions(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String agent) {

        PageRequest pageable = PageRequest.of(page, Math.min(size, 100), Sort.by("createdAt").descending());

        Page<DecisionLog> decisions;
        if (agent != null && !agent.isBlank()) {
            decisions = decisionLogService.findByAgent(agent, pageable);
        } else {
            decisions = decisionLogService.findAll(pageable);
        }

        List<Map<String, Object>> items = decisions.getContent().stream()
                .map(d -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("auditId", d.getId());
                    m.put("agentName", d.getAgentName());
                    m.put("intent", d.getIntent());
                    m.put("status", d.getStatus().name());
                    m.put("createdAt", d.getCreatedAt().toString());
                    return m;
                }).toList();

        return ResponseEntity.ok(Map.of(
                "items", items,
                "page", decisions.getNumber(),
                "totalPages", decisions.getTotalPages(),
                "totalItems", decisions.getTotalElements()));
    }

    @PostMapping("/{auditId}/approve")
    @Operation(summary = "Approve a suggested action plan", description = "Marks a decision as approved. Required before execution.")
    public ResponseEntity<Map<String, Object>> approve(
            @PathVariable String auditId,
            @Valid @RequestBody ApprovalRequest request) {

        DecisionLog log = decisionLogService.approve(auditId, request.approvedBy());

        eventBus.publish(new DecisionApproved(auditId, log.getAgentName(), request.approvedBy()));

        return ResponseEntity.ok(Map.of(
                "auditId", auditId,
                "status", log.getStatus().name(),
                "approvedBy", request.approvedBy(),
                "approvedAt", log.getApprovedAt().toString()));
    }

    @PostMapping("/{auditId}/execute")
    @Operation(summary = "Execute an approved action plan", description = "Executes all actions in the approved plan. Idempotent per action via idempotencyKey.")
    public ResponseEntity<Map<String, Object>> execute(
            @PathVariable String auditId,
            @Valid @RequestBody ExecuteRequest request) {

        List<ExecutionResult> results = actionExecutor.execute(auditId, request.executedBy());

        return ResponseEntity.ok(Map.of(
                "auditId", auditId,
                "results", results));
    }

    public record ApprovalRequest(
            @NotBlank(message = "approvedBy é obrigatório") String approvedBy) {
    }

    public record ExecuteRequest(
            @NotBlank(message = "executedBy é obrigatório") String executedBy) {
    }
}
