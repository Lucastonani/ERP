package com.erp.ia.web;

import com.erp.ia.agent.AgentOrchestrator;
import com.erp.ia.agent.model.AgentRequest;
import com.erp.ia.agent.model.AgentResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.slf4j.MDC;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/agent")
@Tag(name = "Agent", description = "AI Agent cognitive interface — suggest actions via natural language")
public class AgentController {

    private final AgentOrchestrator orchestrator;

    public AgentController(AgentOrchestrator orchestrator) {
        this.orchestrator = orchestrator;
    }

    @PostMapping
    @Operation(summary = "Process an intent", description = "Receives a natural-language intent and returns a structured response with action plan and audit ID")
    public ResponseEntity<AgentResponse> processIntent(@Valid @RequestBody IntentRequest request) {
        String correlationId = MDC.get("correlationId");
        if (correlationId == null) {
            correlationId = UUID.randomUUID().toString();
        }

        AgentRequest agentRequest = new AgentRequest(
                request.intent(),
                request.context() != null ? request.context() : Map.of(),
                request.tenantId() != null ? request.tenantId() : "default",
                request.storeId() != null ? request.storeId() : "default",
                correlationId,
                request.user());

        AgentResponse response = orchestrator.process(agentRequest);
        return ResponseEntity.ok(response);
    }

    public record IntentRequest(
            @NotBlank(message = "Intent é obrigatório") String intent,
            Map<String, Object> context,
            String tenantId,
            String storeId,
            @NotBlank(message = "Usuário é obrigatório") String user) {
    }
}
