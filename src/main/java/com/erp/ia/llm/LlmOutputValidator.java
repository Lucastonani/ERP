package com.erp.ia.llm;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * Validates LLM output against a typed contract (DTO + Bean Validation).
 * If invalid, returns null for the caller to handle (log OUTPUT_INVALID,
 * optional repair).
 */
@Component
public class LlmOutputValidator {

    private static final Logger log = LoggerFactory.getLogger(LlmOutputValidator.class);

    private final ObjectMapper objectMapper;
    private final Validator validator;

    public LlmOutputValidator(ObjectMapper objectMapper, Validator validator) {
        this.objectMapper = objectMapper;
        this.validator = validator;
    }

    /**
     * Parse LLM output string into typed contract and validate.
     * 
     * @return parsed and valid object, or null if invalid
     */
    public <T> T validateAndParse(String llmOutput, Class<T> contractClass) {
        if (llmOutput == null || llmOutput.isBlank()) {
            log.warn("LLM output is empty");
            return null;
        }

        // Strip markdown code fences if present
        String cleaned = llmOutput.strip();
        if (cleaned.startsWith("```")) {
            int firstNewline = cleaned.indexOf('\n');
            int lastFence = cleaned.lastIndexOf("```");
            if (firstNewline > 0 && lastFence > firstNewline) {
                cleaned = cleaned.substring(firstNewline + 1, lastFence).strip();
            }
        }

        try {
            T parsed = objectMapper.readValue(cleaned, contractClass);

            Set<ConstraintViolation<T>> violations = validator.validate(parsed);
            if (!violations.isEmpty()) {
                String errors = violations.stream()
                        .map(v -> v.getPropertyPath() + ": " + v.getMessage())
                        .collect(Collectors.joining(", "));
                log.warn("LLM output validation failed for {}: {}", contractClass.getSimpleName(), errors);
                return null;
            }

            return parsed;

        } catch (Exception e) {
            log.warn("Failed to parse LLM output as {}: {}", contractClass.getSimpleName(), e.getMessage());
            return null;
        }
    }
}
