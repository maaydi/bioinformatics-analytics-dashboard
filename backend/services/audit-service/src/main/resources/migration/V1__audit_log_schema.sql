-- ─────────────────────────────────────────────────────────────────────────────
-- Audit Service — Initial Schema
-- ─────────────────────────────────────────────────────────────────────────────
CREATE SCHEMA IF NOT EXISTS audit_log;
CREATE SEQUENCE audit_log.audit_log_seq INCREMENT BY 500;
CREATE TABLE audit_log.audit_event
(
    id             BIGINT PRIMARY KEY DEFAULT nextval('audit_log.audit_log_seq'),
    event_id       UUID         NOT NULL UNIQUE,
    actor_identity VARCHAR(255),
    actor_type     VARCHAR(30),
    source_service VARCHAR(100) NOT NULL,
    correlation_id VARCHAR(100),
    action         VARCHAR(50)  NOT NULL,
    target_type    VARCHAR(50),
    target_id      VARCHAR(100),
    status         VARCHAR(20),
    ip_address     INET,
    metadata       JSONB,
    created_at     TIMESTAMPTZ DEFAULT NOW()
);