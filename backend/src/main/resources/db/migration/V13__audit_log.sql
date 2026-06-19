CREATE TABLE IF NOT EXISTS audit_log
(
    id            BIGSERIAL PRIMARY KEY,
    actor_user_id BIGINT       NOT NULL REFERENCES app_user (id) ON DELETE CASCADE,
    action_type   VARCHAR(100) NOT NULL,
    target_type   VARCHAR(100),
    target_id     VARCHAR(255),
    status        VARCHAR(20)  NOT NULL,
    ip_address    VARCHAR(45),
    http_method   VARCHAR(10),
    endpoint      VARCHAR(500),
    created_at    TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT check_status CHECK (status IN ('SUCCESS', 'FAILURE')),
    CONSTRAINT check_action_type CHECK (action_type IN (
                                                        'LOGIN', 'LOGOUT', 'TOKEN_REFRESH',
                                                        'FILTER_SAVE', 'FILTER_LOAD', 'FILTER_DELETE',
                                                        'SEARCH_QUERY', 'DATA_EXPORT_CSV', 'DATA_EXPORT_CHART',
                                                        'DETAIL_VIEW', 'IMPORT_UPLOAD', 'IMPORT_CANCEL',
                                                        'COMPARE_ANALYTICS', 'PASSWORD_CHANGE', 'PROFILE_UPDATE',
                                                        'ADMIN_DELETE_USER_FILTER'
        ))
);

CREATE INDEX idx_audit_user_id_created_at ON audit_log (actor_user_id, created_at DESC);
CREATE INDEX idx_audit_created_at ON audit_log (created_at DESC);
