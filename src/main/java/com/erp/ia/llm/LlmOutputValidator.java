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
 *
 * Handles common LLM output quirks:
 * - Markdown code fences (```json ... ```)
 * - Explanatory text before/after JSON
 * - Extra whitespace
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

        String cleaned = extractJson(llmOutput);
        if (cleaned == null) {
            log.warn("No JSON found in LLM output for {}", contractClass.getSimpleName());
            return null;
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

    /**
     * Extracts JSON from LLM output, handling:
     * 1. Markdown code fences: ```json\n{...}\n```
     * 2. Text before/after JSON: "Here is the result: {...} Hope that helps!"
     * 3. Clean JSON as-is
     */
    String extractJson(String raw) {
        String stripped = raw.strip();

        // Strategy 1: Strip markdown code fences
        if (stripped.startsWith("```")) {
            int firstNewline = stripped.indexOf('\n');
            int lastFence = stripped.lastIndexOf("```");
            if (firstNewline > 0 && lastFence > firstNewline) {
                return stripped.substring(firstNewline + 1, lastFence).strip();
            }
        }

        // Strategy 2: Already clean JSON object or array
        if (stripped.startsWith("{") || stripped.startsWith("[")) {
            return stripped;
        }

        // Strategy 3: Extract first JSON object {...} from surrounding text
        int braceStart = stripped.indexOf('{');
        int bracketStart = stripped.indexOf('[');

        int start = -1;
        char openChar;
        char closeChar;

        if (braceStart >= 0 && (bracketStart < 0 || braceStart < bracketStart)) {
            start = braceStart;
            openChar = '{';
            closeChar = '}';
        } else if (bracketStart >= 0) {
            start = bracketStart;
            openChar = '[';
            closeChar = ']';
        } else {
            return null; // no JSON structure found
        }

        // Find the matching closing bracket (handle nesting)
        int depth = 0;
        for (int i = start; i < stripped.length(); i++) {
            char c = stripped.charAt(i);
            if (c == openChar)
                depth++;
            else if (c == closeChar)
                depth--;
            if (depth == 0) {
                return stripped.substring(start, i + 1);
            }
        }

        return null; // unbalanced brackets
    }
}
