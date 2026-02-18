package com.erp.ia.web;

import com.erp.ia.agent.AgentOrchestrator;
import com.erp.ia.agent.model.ActionPlan;
import com.erp.ia.agent.model.AgentResponse;
import com.erp.ia.web.AgentController.IntentRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AgentController.class)
@AutoConfigureMockMvc(addFilters = false)
class AgentControllerTest {

        @Autowired
        private MockMvc mockMvc;

        @Autowired
        private ObjectMapper objectMapper;

        @MockitoBean
        private AgentOrchestrator orchestrator;

        @Test
        void processIntent_validRequest_returns200() throws Exception {
                AgentResponse mockResponse = new AgentResponse(
                                "Análise concluída", ActionPlan.empty("Nenhuma ação"),
                                List.of(), "audit-123");
                when(orchestrator.process(any())).thenReturn(mockResponse);

                String json = """
                                {"intent": "reorder", "user": "admin"}
                                """;

                mockMvc.perform(post("/api/v1/agent")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(json))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.auditId").value("audit-123"))
                                .andExpect(jsonPath("$.response").value("Análise concluída"));
        }

        @Test
        void processIntent_missingIntent_returns400() throws Exception {
                String json = """
                                {"user": "admin"}
                                """;

                mockMvc.perform(post("/api/v1/agent")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(json))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
        }

        @Test
        void processIntent_missingUser_returns400() throws Exception {
                String json = """
                                {"intent": "reorder"}
                                """;

                mockMvc.perform(post("/api/v1/agent")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(json))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
        }

        @Test
        void processIntent_blankIntent_returns400() throws Exception {
                String json = """
                                {"intent": "   ", "user": "admin"}
                                """;

                mockMvc.perform(post("/api/v1/agent")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(json))
                                .andExpect(status().isBadRequest());
        }

        @Test
        void processIntent_unknownAgent_returns404() throws Exception {
                when(orchestrator.process(any()))
                                .thenThrow(new IllegalArgumentException("No agent found for intent: xyz"));

                String json = """
                                {"intent": "xyz", "user": "admin"}
                                """;

                mockMvc.perform(post("/api/v1/agent")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(json))
                                .andExpect(status().isNotFound())
                                .andExpect(jsonPath("$.error").value("NOT_FOUND"));
        }
}
