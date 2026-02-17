-- =============================================
-- V3: Audit schema — decision logs + structured child tables
-- =============================================

CREATE TABLE decision_logs (
    id              VARCHAR(36)     PRIMARY KEY,  -- UUID
    correlation_id  VARCHAR(36),
    agent_name      VARCHAR(100)    NOT NULL,
    intent          VARCHAR(500)    NOT NULL,
    prompt_name     VARCHAR(100),
    prompt_version  INT,
    input_data      TEXT,           -- JSON
    llm_request     TEXT,           -- JSON
    llm_response    TEXT,           -- JSON
    action_plan     TEXT,           -- JSON
    status          VARCHAR(30)     NOT NULL DEFAULT 'SUGGESTED', -- SUGGESTED, APPROVED, EXECUTED, REJECTED, OUTPUT_INVALID
    approved_by     VARCHAR(100),
    approved_at     TIMESTAMP,
    tenant_id       VARCHAR(50)     NOT NULL DEFAULT 'default',
    store_id        VARCHAR(50),
    created_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE decision_tool_calls (
    id              BIGSERIAL       PRIMARY KEY,
    decision_log_id VARCHAR(36)     NOT NULL REFERENCES decision_logs(id) ON DELETE CASCADE,
    tool_name       VARCHAR(100)    NOT NULL,
    input_json      TEXT,
    output_json     TEXT,
    duration_ms     BIGINT,
    called_at       TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE decision_policy_results (
    id              BIGSERIAL       PRIMARY KEY,
    decision_log_id VARCHAR(36)     NOT NULL REFERENCES decision_logs(id) ON DELETE CASCADE,
    rule_name       VARCHAR(100)    NOT NULL,
    result          VARCHAR(20)     NOT NULL, -- PASS, BLOCKED
    reason          VARCHAR(500),
    evaluated_at    TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_decision_logs_agent ON decision_logs(agent_name);
CREATE INDEX idx_decision_logs_status ON decision_logs(status);
CREATE INDEX idx_decision_logs_correlation ON decision_logs(correlation_id);
CREATE INDEX idx_decision_logs_tenant ON decision_logs(tenant_id);
CREATE INDEX idx_decision_logs_created ON decision_logs(created_at);
CREATE INDEX idx_decision_tool_calls_log ON decision_tool_calls(decision_log_id);
CREATE INDEX idx_decision_policy_results_log ON decision_policy_results(decision_log_id);
