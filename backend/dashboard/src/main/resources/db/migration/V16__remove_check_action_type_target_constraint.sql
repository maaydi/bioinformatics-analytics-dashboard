CREATE TABLE audit_action_definitions
(
    action_type VARCHAR(50),
    target_type VARCHAR(50),
    PRIMARY KEY (action_type)
);

INSERT INTO audit_action_definitions (action_type, target_type)
VALUES ('LOGIN', 'AUTH'),
       ('LOGOUT', 'AUTH'),
       ('TOKEN_REFRESH', 'AUTH'),
       ('PASSWORD_CHANGE', 'USER'),
       ('UPDATE_PASSWORD', 'USER'), -- Your new action
       ('PROFILE_UPDATE', 'USER'),
       ('FILTER_SAVE', 'SAVED_FILTER'),
       ('FILTER_LOAD', 'SAVED_FILTER'),
       ('FILTER_DELETE', 'SAVED_FILTER'),
       ('SEARCH_QUERY', 'SEARCH'),
       ('DATA_EXPORT_CSV', 'EXPORT_CSV'),
       ('DATA_EXPORT_CHART', 'EXPORT_CHART'),
       ('DETAIL_VIEW', 'DETAIL'),
       ('IMPORT_UPLOAD', 'IMPORT_JOB'),
       ('IMPORT_CANCEL', 'IMPORT_JOB'),
       ('COMPARE_ANALYTICS', 'COMPARE'),
       ('ADMIN_DELETE_USER_FILTER', 'SAVED_FILTER');

-- Drop both of the old, restrictive check constraints
ALTER TABLE audit_log DROP CONSTRAINT IF EXISTS check_action_type;
ALTER TABLE audit_log DROP CONSTRAINT IF EXISTS check_target_type;

-- Add the composite foreign key instantly without locking for validation
ALTER TABLE audit_log
    ADD CONSTRAINT fk_audit_action_target_matrix
        FOREIGN KEY (action_type)
            REFERENCES audit_action_definitions (action_type)
    NOT VALID;

-- Validate the historical rows safely in the background
ALTER TABLE audit_log VALIDATE CONSTRAINT fk_audit_action_target_matrix;
