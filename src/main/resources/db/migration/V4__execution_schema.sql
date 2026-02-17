-- =============================================
-- V4: Execution schema — idempotency tracking
-- =============================================

CREATE TABLE executed_actions (
    id              BIGSERIAL       PRIMARY KEY,
    idempotency_key VARCHAR(36)     NOT NULL UNIQUE,
    audit_id        VARCHAR(36)     NOT NULL REFERENCES decision_logs(id),
    action_type     VARCHAR(50)     NOT NULL,
    status          VARCHAR(20)     NOT NULL DEFAULT 'SUCCESS', -- SUCCESS, FAILED
    result_json     TEXT,
    executed_by     VARCHAR(100),
    tenant_id       VARCHAR(50)     NOT NULL DEFAULT 'default',
    executed_at     TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_executed_actions_audit ON executed_actions(audit_id);
CREATE INDEX idx_executed_actions_key ON executed_actions(idempotency_key);
