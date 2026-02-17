package com.erp.ia.tool;

import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Registry of all available agent tools.
 */
@Component
public class ToolRegistry {

    private final Map<String, AgentTool<?, ?>> tools = new LinkedHashMap<>();

    public ToolRegistry(List<AgentTool<?, ?>> toolList) {
        for (AgentTool<?, ?> tool : toolList) {
            tools.put(tool.getName(), tool);
        }
    }

    public Optional<AgentTool<?, ?>> findByName(String name) {
        return Optional.ofNullable(tools.get(name));
    }

    public Collection<AgentTool<?, ?>> getAll() {
        return Collections.unmodifiableCollection(tools.values());
    }
}
