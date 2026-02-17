package com.erp.ia.llm;

import java.util.List;
import java.util.Map;

/**
 * Port (interface) for LLM providers. Core domain never depends on a specific
 * LLM.
 */
public interface LlmPort {

    LlmResponse complete(LlmRequest request);

    record LlmRequest(
            String model,
            List<Message> messages,
            double temperature,
            int maxTokens) {
        public record Message(String role, String content) {
        }
    }

    record LlmResponse(
            String content,
            String model,
            String finishReason,
            Usage usage,
            boolean error,
            String errorMessage) {
        public record Usage(int promptTokens, int completionTokens, int totalTokens) {
        }

        public static LlmResponse ofError(String message) {
            return new LlmResponse(null, null, null, null, true, message);
        }
    }
}
