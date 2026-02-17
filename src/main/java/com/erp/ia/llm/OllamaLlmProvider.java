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
 * Local LLM provider via Ollama HTTP API.
 */
@Component
@ConditionalOnProperty(name = "llm.provider", havingValue = "ollama")
public class OllamaLlmProvider implements LlmPort {

    private static final Logger log = LoggerFactory.getLogger(OllamaLlmProvider.class);

    private final String baseUrl;
    private final String model;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    public OllamaLlmProvider(
            @Value("${llm.ollama.base-url:http://localhost:11434}") String baseUrl,
            @Value("${llm.ollama.model:llama3}") String model,
            ObjectMapper objectMapper) {
        this.baseUrl = baseUrl;
        this.model = model;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
    }

    @Override
    public LlmResponse complete(LlmRequest request) {
        try {
            String effectiveModel = request.model() != null ? request.model() : this.model;

            List<Map<String, String>> messages = request.messages().stream()
                    .map(m -> Map.of("role", m.role(), "content", m.content()))
                    .toList();

            Map<String, Object> body = Map.of(
                    "model", effectiveModel,
                    "messages", messages,
                    "stream", false,
                    "options", Map.of(
                            "temperature", request.temperature(),
                            "num_predict", request.maxTokens()));

            String jsonBody = objectMapper.writeValueAsString(body);

            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/api/chat"))
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofSeconds(60))
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .build();

            HttpResponse<String> httpResponse = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());

            if (httpResponse.statusCode() >= 400) {
                return LlmResponse.ofError("Ollama error " + httpResponse.statusCode() + ": " + httpResponse.body());
            }

            JsonNode root = objectMapper.readTree(httpResponse.body());
            String content = root.path("message").path("content").asText("");

            return new LlmResponse(content, effectiveModel, "stop",
                    new LlmResponse.Usage(0, 0, 0), false, null);

        } catch (Exception e) {
            log.error("Ollama call failed: {}", e.getMessage(), e);
            return LlmResponse.ofError("Ollama call failed: " + e.getMessage());
        }
    }
}
