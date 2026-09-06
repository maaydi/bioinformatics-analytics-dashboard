-- ─────────────────────────────────────────────────────────────────────────────
-- Auth Service — Initial Schema
-- Scope: auth schema only (isolated from other services)
-- Strategy: PRIMARY datasource only (all auth writes go to primary)
-- ─────────────────────────────────────────────────────────────────────────────

CREATE SCHEMA IF NOT EXISTS auth;

-- ── app_user ──────────────────────────────────────────────────────────────────
-- Central identity table. role stored as a plain VARCHAR to allow future RBAC
-- without a schema change; application enforces valid values via enum mapping.
CREATE TABLE auth.app_user
(
    id              BIGSERIAL PRIMARY KEY,
    username        VARCHAR(50)  NOT NULL UNIQUE,
    password        VARCHAR(255) NOT NULL,
    role            VARCHAR(20)  NOT NULL DEFAULT 'ROLE_USER'
        CHECK (role IN ('ROLE_USER', 'ROLE_ADMIN')),
    status          VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE'
        CHECK (status IN ('CREATED', 'ACTIVE', 'DISABLED', 'DELETED')),
    failed_attempts INT          NOT NULL DEFAULT 0,
    locked_until    TIMESTAMPTZ,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_app_user_username ON auth.app_user (username);
CREATE INDEX idx_app_user_status ON auth.app_user (status) WHERE status <> 'DELETED';

-- ── refresh_token ─────────────────────────────────────────────────────────────
-- Persists hashed refresh tokens so revocation (logout) is durable across
-- restarts.  token_hash stores SHA-256(raw_token) — never the raw value.
CREATE TABLE auth.refresh_token
(
    id         BIGSERIAL PRIMARY KEY,
    user_id    BIGINT       NOT NULL REFERENCES auth.app_user (id) ON DELETE CASCADE,
    token_hash VARCHAR(100) NOT NULL UNIQUE,
    expires_at TIMESTAMPTZ  NOT NULL,
    revoked    BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_rt_user_id ON auth.refresh_token (user_id);
CREATE INDEX idx_rt_token_hash ON auth.refresh_token (token_hash);
CREATE INDEX idx_rt_active ON auth.refresh_token (user_id, revoked, expires_at)
    WHERE revoked = FALSE;

-- ── seed: local development test accounts ─────────────────────────────────────
-- Passwords are Bcrypt hashes of 'password' (ROLE_USER) and 'admin123' (ROLE_ADMIN).
-- These rows are inserted only when the 'dev' profile is active; the CHECK
-- constraint on role is intentionally satisfied by both values.
INSERT INTO auth.app_user (username, password, role, status)
VALUES ('user_test',
        '$2b$10$sd7Wth3x55Z/0F/iZ9qyzu5g0Ndz25F3Beez6qBPAMHQY7C.88Bsu',
        'ROLE_USER',
        'ACTIVE'),
       ('admin_test',
        '$2b$10$oFip6L2K1z7zDJHFvehoy.axDZHiFVuMZK4Xx8G9pHRkoGqewgSQa',
        'ROLE_ADMIN',
        'ACTIVE')
ON CONFLICT (username) DO NOTHING;

