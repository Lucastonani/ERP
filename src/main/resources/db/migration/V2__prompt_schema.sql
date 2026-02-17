-- =============================================
-- V2: Prompt engine schema
-- =============================================

CREATE TABLE prompt_templates (
    id              BIGSERIAL       PRIMARY KEY,
    name            VARCHAR(100)    NOT NULL,
    version         INT             NOT NULL DEFAULT 1,
    content         TEXT            NOT NULL,
    variables       TEXT,           -- JSON array of variable names
    status          VARCHAR(20)     NOT NULL DEFAULT 'ACTIVE', -- DRAFT, ACTIVE, DEPRECATED
    effective_from  TIMESTAMP,
    change_note     VARCHAR(500),
    tenant_id       VARCHAR(50)     NOT NULL DEFAULT 'default',
    created_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(name, version, tenant_id)
);

CREATE INDEX idx_prompt_templates_name ON prompt_templates(name, tenant_id);
CREATE INDEX idx_prompt_templates_status ON prompt_templates(status);
