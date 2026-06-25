ALTER TABLE app_user
    ADD COLUMN status VARCHAR(20) DEFAULT 'ACTIVE';

UPDATE app_user
SET status = 'ACTIVE'
WHERE status IS NULL;

ALTER TABLE app_user
    ALTER COLUMN status SET NOT NULL;

ALTER TABLE app_user
    ADD CONSTRAINT chk_user_status
        CHECK (status IN ('CREATED', 'ACTIVE', 'DISABLED', 'DELETED'));
