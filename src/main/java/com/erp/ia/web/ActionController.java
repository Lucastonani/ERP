package com.erp.ia.web;

import com.erp.ia.audit.DecisionLogService;
import com.erp.ia.audit.model.DecisionLog;
import com.erp.ia.event.EventBus;
import com.erp.ia.event.model.DecisionApproved;
import com.erp.ia.execution.ActionExecutor;
import com.erp.ia.execution.ExecutionResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/actions")
@Tag(name = "Actions", description = "Approve and execute AI-suggested actions")
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

    @PostMapping("/{auditId}/approve")
    @Operation(summary = "Approve a suggested action plan", description = "Marks a decision as approved. Required before execution.")
    public ResponseEntity<Map<String, Object>> approve(
            @PathVariable String auditId,
            @RequestBody ApprovalRequest request) {

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
            @RequestBody ExecuteRequest request) {

        List<ExecutionResult> results = actionExecutor.execute(auditId, request.executedBy());

        return ResponseEntity.ok(Map.of(
                "auditId", auditId,
                "results", results));
    }

    public record ApprovalRequest(String approvedBy) {
    }

    public record ExecuteRequest(String executedBy) {
    }
}
