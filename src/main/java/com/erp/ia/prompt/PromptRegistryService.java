package com.erp.ia.prompt;

import com.erp.ia.prompt.model.PromptTemplate;
import com.erp.ia.prompt.model.PromptTemplate.PromptStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Versioned prompt registry: create, retrieve active, rollback, manage
 * lifecycle.
 */
@Service
public class PromptRegistryService {

    private final PromptRepository promptRepository;

    public PromptRegistryService(PromptRepository promptRepository) {
        this.promptRepository = promptRepository;
    }

    /** Get the latest ACTIVE prompt by name. */
    public Optional<PromptTemplate> getActivePrompt(String name, String tenantId) {
        return promptRepository.findFirstByNameAndStatusAndTenantIdOrderByVersionDesc(
                name, PromptStatus.ACTIVE, tenantId);
    }

    /** Get a specific version of a prompt. */
    public Optional<PromptTemplate> getPrompt(String name, int version, String tenantId) {
        return promptRepository.findByNameAndVersionAndTenantId(name, version, tenantId);
    }

    /** List all versions of a prompt (newest first). */
    public List<PromptTemplate> listVersions(String name, String tenantId) {
        return promptRepository.findByNameAndTenantIdOrderByVersionDesc(name, tenantId);
    }

    /**
     * Create a new version of a prompt.
     * - Auto-increments version.
     * - Deprecates previous active versions.
     * - New version starts as ACTIVE.
     */
    @Transactional
    public PromptTemplate createVersion(String name, String content, String variables,
            String changeNote, String tenantId) {
        List<PromptTemplate> existing = promptRepository.findByNameAndTenantIdOrderByVersionDesc(name, tenantId);

        int nextVersion = existing.isEmpty() ? 1 : existing.get(0).getVersion() + 1;

        // Deprecate current active versions
        existing.stream()
                .filter(p -> p.getStatus() == PromptStatus.ACTIVE)
                .forEach(p -> {
                    p.setStatus(PromptStatus.DEPRECATED);
                    promptRepository.save(p);
                });

        PromptTemplate newPrompt = new PromptTemplate(name, nextVersion, content);
        newPrompt.setVariables(variables);
        newPrompt.setStatus(PromptStatus.ACTIVE);
        newPrompt.setEffectiveFrom(Instant.now());
        newPrompt.setChangeNote(changeNote);
        newPrompt.setTenantId(tenantId);
        return promptRepository.save(newPrompt);
    }

    /**
     * Rollback: activate a previous version and deprecate the current active.
     */
    @Transactional
    public PromptTemplate rollback(String name, int targetVersion, String tenantId) {
        PromptTemplate target = promptRepository.findByNameAndVersionAndTenantId(name, targetVersion, tenantId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Prompt '" + name + "' version " + targetVersion + " not found"));

        // Deprecate current active(s)
        List<PromptTemplate> all = promptRepository.findByNameAndTenantIdOrderByVersionDesc(name, tenantId);
        all.stream()
                .filter(p -> p.getStatus() == PromptStatus.ACTIVE)
                .forEach(p -> {
                    p.setStatus(PromptStatus.DEPRECATED);
                    promptRepository.save(p);
                });

        target.setStatus(PromptStatus.ACTIVE);
        target.setEffectiveFrom(Instant.now());
        return promptRepository.save(target);
    }
}
