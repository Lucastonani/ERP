package com.erp.ia.agent;

import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Registry of all available agents. Resolves intents to agents.
 */
@Component
public class AgentRegistry {

    private final Map<String, AgentDefinition> agentsByName = new LinkedHashMap<>();
    private final Map<String, AgentDefinition> agentsByIntent = new HashMap<>();

    public AgentRegistry(List<AgentDefinition> agents) {
        for (AgentDefinition agent : agents) {
            agentsByName.put(agent.getName(), agent);
            for (String intent : agent.getSupportedIntents()) {
                agentsByIntent.put(intent.toLowerCase(), agent);
            }
        }
    }

    public Optional<AgentDefinition> findByIntent(String intent) {
        // Exact match first
        AgentDefinition exact = agentsByIntent.get(intent.toLowerCase());
        if (exact != null)
            return Optional.of(exact);

        // Partial match (intent contains keyword)
        return agentsByIntent.entrySet().stream()
                .filter(e -> intent.toLowerCase().contains(e.getKey()))
                .map(Map.Entry::getValue)
                .findFirst();
    }

    public Optional<AgentDefinition> findByName(String name) {
        return Optional.ofNullable(agentsByName.get(name));
    }

    public Collection<AgentDefinition> getAll() {
        return Collections.unmodifiableCollection(agentsByName.values());
    }
}
