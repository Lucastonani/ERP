package com.erp.ia.prompt.model;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "prompt_templates", uniqueConstraints = {
        @UniqueConstraint(columnNames = { "name", "version", "tenant_id" })
})
public class PromptTemplate {

    public enum PromptStatus {
        DRAFT, ACTIVE, DEPRECATED
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false)
    private int version = 1;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(columnDefinition = "TEXT")
    private String variables; // JSON array of variable names

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PromptStatus status = PromptStatus.ACTIVE;

    @Column(name = "effective_from")
    private Instant effectiveFrom;

    @Column(name = "change_note", length = 500)
    private String changeNote;

    @Column(name = "tenant_id", nullable = false, length = 50)
    private String tenantId = "default";

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public PromptTemplate() {
    }

    public PromptTemplate(String name, int version, String content) {
        this.name = name;
        this.version = version;
        this.content = content;
    }

    /**
     * Renders the prompt by replacing {{variable}} placeholders.
     */
    public String render(java.util.Map<String, String> vars) {
        String rendered = content;
        for (var entry : vars.entrySet()) {
            rendered = rendered.replace("{{" + entry.getKey() + "}}", entry.getValue());
        }
        return rendered;
    }

    // --- Getters & Setters ---

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getVariables() {
        return variables;
    }

    public void setVariables(String variables) {
        this.variables = variables;
    }

    public PromptStatus getStatus() {
        return status;
    }

    public void setStatus(PromptStatus status) {
        this.status = status;
    }

    public Instant getEffectiveFrom() {
        return effectiveFrom;
    }

    public void setEffectiveFrom(Instant effectiveFrom) {
        this.effectiveFrom = effectiveFrom;
    }

    public String getChangeNote() {
        return changeNote;
    }

    public void setChangeNote(String changeNote) {
        this.changeNote = changeNote;
    }

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
