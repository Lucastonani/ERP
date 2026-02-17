package com.erp.ia.llm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * Cloud LLM provider (OpenAI-compatible API).
 * Includes timeout, retry with exponential backoff, and circuit-breaker.
 */
@Component
@ConditionalOnProperty(name = "llm.provider", havingValue = "cloud")
public class CloudLlmProvider implements LlmPort {

    private static final Logger log = LoggerFactory.getLogger(CloudLlmProvider.class);

    private final String baseUrl;
    private final String apiKey;
    private final String model;
    private final Duration connectTimeout;
    private final Duration readTimeout;
    private final int maxRetries;
    private final int circuitBreakerThreshold;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    private int consecutiveFailures = 0;
    private boolean circuitOpen = false;
    private long circuitOpenedAt = 0;
    private static final long CIRCUIT_HALF_OPEN_MS = 30_000;

    public CloudLlmProvider(
            @Value("${llm.cloud.base-url}") String baseUrl,
            @Value("${llm.cloud.api-key}") String apiKey,
            @Value("${llm.cloud.model}") String model,
            @Value("${llm.timeout-connect:5s}") Duration connectTimeout,
            @Value("${llm.timeout-read:30s}") Duration readTimeout,
            @Value("${llm.retry-max:2}") int maxRetries,
            @Value("${llm.circuit-breaker-threshold:5}") int circuitBreakerThreshold,
            ObjectMapper objectMapper) {
        this.baseUrl = baseUrl;
        this.apiKey = apiKey;
        this.model = model;
        this.connectTimeout = connectTimeout;
        this.readTimeout = readTimeout;
        this.maxRetries = maxRetries;
        this.circuitBreakerThreshold = circuitBreakerThreshold;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(connectTimeout)
                .build();
    }

    @Override
    public LlmResponse complete(LlmRequest request) {
        if (isCircuitOpen()) {
            log.warn("Circuit breaker OPEN — returning fallback");
            return LlmResponse.ofError("LLM service temporarily unavailable (circuit breaker open)");
        }

        String effectiveModel = request.model() != null ? request.model() : this.model;

        for (int attempt = 0; attempt <= maxRetries; attempt++) {
            try {
                LlmResponse response = doRequest(request, effectiveModel);
                onSuccess();
                return response;
            } catch (Exception e) {
                log.warn("LLM call attempt {} failed: {}", attempt + 1, e.getMessage());
                if (attempt < maxRetries) {
                    sleepBackoff(attempt);
                } else {
                    onFailure();
                    return LlmResponse
                            .ofError("LLM call failed after " + (maxRetries + 1) + " attempts: " + e.getMessage());
                }
            }
        }
        return LlmResponse.ofError("Unexpected error in LLM call");
    }

    private LlmResponse doRequest(LlmRequest request, String effectiveModel) throws Exception {
        List<Map<String, String>> messages = request.messages().stream()
                .map(m -> Map.of("role", m.role(), "content", m.content()))
                .toList();

        Map<String, Object> body = Map.of(
                "model", effectiveModel,
                "messages", messages,
                "temperature", request.temperature(),
                "max_tokens", request.maxTokens());

        String jsonBody = objectMapper.writeValueAsString(body);

        HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/chat/completions"))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + apiKey)
                .timeout(readTimeout)
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();

        HttpResponse<String> httpResponse = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());

        if (httpResponse.statusCode() >= 500) {
            throw new RuntimeException("Server error: " + httpResponse.statusCode());
        }
        if (httpResponse.statusCode() >= 400) {
            return LlmResponse.ofError("LLM API error " + httpResponse.statusCode() + ": " + httpResponse.body());
        }

        return parseResponse(httpResponse.body(), effectiveModel);
    }

    private LlmResponse parseResponse(String body, String effectiveModel) throws Exception {
        JsonNode root = objectMapper.readTree(body);
        JsonNode choices = root.path("choices");
        String content = "";
        String finishReason = "";
        if (choices.isArray() && !choices.isEmpty()) {
            content = choices.get(0).path("message").path("content").asText("");
            finishReason = choices.get(0).path("finish_reason").asText("");
        }
        JsonNode usage = root.path("usage");
        return new LlmResponse(
                content, effectiveModel, finishReason,
                new LlmResponse.Usage(
                        usage.path("prompt_tokens").asInt(0),
                        usage.path("completion_tokens").asInt(0),
                        usage.path("total_tokens").asInt(0)),
                false, null);
    }

    private boolean isCircuitOpen() {
        if (!circuitOpen)
            return false;
        if (System.currentTimeMillis() - circuitOpenedAt > CIRCUIT_HALF_OPEN_MS) {
            log.info("Circuit breaker half-open — allowing one attempt");
            circuitOpen = false;
            return false;
        }
        return true;
    }

    private synchronized void onSuccess() {
        consecutiveFailures = 0;
        circuitOpen = false;
    }

    private synchronized void onFailure() {
        consecutiveFailures++;
        if (consecutiveFailures >= circuitBreakerThreshold) {
            circuitOpen = true;
            circuitOpenedAt = System.currentTimeMillis();
            log.error("Circuit breaker OPENED after {} consecutive failures", consecutiveFailures);
        }
    }

    private void sleepBackoff(int attempt) {
        try {
            Thread.sleep((long) Math.pow(2, attempt) * 500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
