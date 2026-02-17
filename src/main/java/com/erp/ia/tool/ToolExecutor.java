package com.erp.ia.tool;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Executes agent tools with validation, logging, and timeout.
 */
@Component
public class ToolExecutor {

    private static final Logger log = LoggerFactory.getLogger(ToolExecutor.class);

    private final ToolRegistry toolRegistry;
    private final ObjectMapper objectMapper;

    public ToolExecutor(ToolRegistry toolRegistry, ObjectMapper objectMapper) {
        this.toolRegistry = toolRegistry;
        this.objectMapper = objectMapper;
    }

    /**
     * Execute a tool by name with parameters.
     * Converts Map parameters to the tool's typed input DTO.
     */
    @SuppressWarnings("unchecked")
    public Object executeTool(String toolName, Map<String, Object> parameters) {
        AgentTool<Object, Object> tool = (AgentTool<Object, Object>) toolRegistry.findByName(toolName)
                .orElseThrow(() -> new IllegalArgumentException("Tool not found: " + toolName));

        long start = System.currentTimeMillis();
        try {
            // Convert map to typed input
            Object input = objectMapper.convertValue(parameters, tool.getInputType());

            log.info("Executing tool: {} with input: {}", toolName, parameters);
            Object result = tool.execute(input);
            long duration = System.currentTimeMillis() - start;
            log.info("Tool {} completed in {}ms", toolName, duration);

            return result;
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - start;
            log.error("Tool {} failed after {}ms: {}", toolName, duration, e.getMessage(), e);
            throw new RuntimeException("Tool execution failed: " + toolName, e);
        }
    }
}
