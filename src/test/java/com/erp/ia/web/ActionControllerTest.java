package com.erp.ia.web;

import com.erp.ia.audit.DecisionLogService;
import com.erp.ia.audit.model.DecisionLog;
import com.erp.ia.event.EventBus;
import com.erp.ia.execution.ActionExecutor;
import com.erp.ia.execution.ExecutionResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ActionController.class)
@AutoConfigureMockMvc(addFilters = false)
class ActionControllerTest {

        @Autowired
        private MockMvc mockMvc;

        @Autowired
        private ObjectMapper objectMapper;

        @MockitoBean
        private DecisionLogService decisionLogService;

        @MockitoBean
        private ActionExecutor actionExecutor;

        @MockitoBean
        private EventBus eventBus;

        private DecisionLog buildDecision(String id, DecisionLog.DecisionStatus status) {
                DecisionLog log = new DecisionLog();
                log.setId(id);
                log.setAgentName("purchasing-agent");
                log.setIntent("reorder");
                log.setStatus(status);
                log.setTenantId("default");
                log.setCreatedAt(Instant.now());
                return log;
        }

        // --- GET /{auditId} ---

        @Test
        void getDecision_exists_returns200() throws Exception {
                DecisionLog decision = buildDecision("audit-1", DecisionLog.DecisionStatus.SUGGESTED);
                when(decisionLogService.findById("audit-1")).thenReturn(Optional.of(decision));

                mockMvc.perform(get("/api/v1/actions/audit-1"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.auditId").value("audit-1"))
                                .andExpect(jsonPath("$.agentName").value("purchasing-agent"))
                                .andExpect(jsonPath("$.status").value("SUGGESTED"));
        }

        @Test
        void getDecision_notFound_returns404() throws Exception {
                when(decisionLogService.findById("no-exist")).thenReturn(Optional.empty());

                mockMvc.perform(get("/api/v1/actions/no-exist"))
                                .andExpect(status().isNotFound())
                                .andExpect(jsonPath("$.error").value("NOT_FOUND"));
        }

        // --- GET / (list) ---

        @Test
        void listDecisions_returnsPage() throws Exception {
                DecisionLog d1 = buildDecision("a1", DecisionLog.DecisionStatus.SUGGESTED);
                DecisionLog d2 = buildDecision("a2", DecisionLog.DecisionStatus.EXECUTED);

                when(decisionLogService.findAll(any(Pageable.class)))
                                .thenReturn(new PageImpl<>(List.of(d1, d2)));

                mockMvc.perform(get("/api/v1/actions"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.items.length()").value(2))
                                .andExpect(jsonPath("$.totalItems").value(2));
        }

        // --- POST /{auditId}/approve ---

        @Test
        void approve_validRequest_returns200() throws Exception {
                DecisionLog approved = buildDecision("audit-1", DecisionLog.DecisionStatus.APPROVED);
                approved.setApprovedBy("manager");
                approved.setApprovedAt(Instant.now());
                when(decisionLogService.approve("audit-1", "manager")).thenReturn(approved);

                mockMvc.perform(post("/api/v1/actions/audit-1/approve")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"approvedBy\": \"manager\"}"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.status").value("APPROVED"))
                                .andExpect(jsonPath("$.approvedBy").value("manager"));
        }

        @Test
        void approve_missingApprovedBy_returns400() throws Exception {
                mockMvc.perform(post("/api/v1/actions/audit-1/approve")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{}"))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
        }

        @Test
        void approve_notFound_returns404() throws Exception {
                when(decisionLogService.approve("no-exist", "manager"))
                                .thenThrow(new IllegalArgumentException("Decision not found: no-exist"));

                mockMvc.perform(post("/api/v1/actions/no-exist/approve")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"approvedBy\": \"manager\"}"))
                                .andExpect(status().isNotFound())
                                .andExpect(jsonPath("$.error").value("NOT_FOUND"));
        }

        // --- POST /{auditId}/execute ---

        @Test
        void execute_validRequest_returns200() throws Exception {
                when(actionExecutor.execute("audit-1", "manager"))
                                .thenReturn(List.of(ExecutionResult.success("DRAFT_PURCHASE_ORDER", "OK", null)));

                mockMvc.perform(post("/api/v1/actions/audit-1/execute")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"executedBy\": \"manager\"}"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.auditId").value("audit-1"))
                                .andExpect(jsonPath("$.results.length()").value(1));
        }

        @Test
        void execute_notApproved_returns409() throws Exception {
                when(actionExecutor.execute("audit-1", "manager"))
                                .thenThrow(new IllegalStateException("Decision must be APPROVED before execution."));

                mockMvc.perform(post("/api/v1/actions/audit-1/execute")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"executedBy\": \"manager\"}"))
                                .andExpect(status().isConflict())
                                .andExpect(jsonPath("$.error").value("CONFLICT"));
        }

        @Test
        void execute_missingExecutedBy_returns400() throws Exception {
                mockMvc.perform(post("/api/v1/actions/audit-1/execute")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{}"))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
        }
}
