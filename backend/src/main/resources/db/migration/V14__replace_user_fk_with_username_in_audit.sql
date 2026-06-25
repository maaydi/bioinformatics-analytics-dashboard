-- 'audit_log_actor_user_id_fkey' is the standard auto-generated name in PostgreSQL.
ALTER TABLE audit_log
    DROP CONSTRAINT IF EXISTS audit_log_actor_user_id_fkey;

ALTER TABLE audit_log
    ALTER COLUMN actor_user_id DROP NOT NULL;

ALTER TABLE audit_log
    ADD COLUMN actor_username VARCHAR(255);

UPDATE audit_log
SET actor_username = 'UNKNOWN_LEGACY_USER'
WHERE actor_username IS NULL;

ALTER TABLE audit_log
    ALTER COLUMN actor_username SET NOT NULL;
-- Drop previous created indices
DROP INDEX IF EXISTS idx_audit_user_id_created_at;
DROP INDEX IF EXISTS idx_audit_created_at;
-- create new indices
CREATE INDEX idx_audit_user_id_created_at ON audit_log (actor_user_id, created_at DESC);
CREATE INDEX idx_audit_user_name_created_at ON audit_log (actor_username, created_at DESC);
CREATE INDEX idx_audit_created_at ON audit_log (created_at DESC);