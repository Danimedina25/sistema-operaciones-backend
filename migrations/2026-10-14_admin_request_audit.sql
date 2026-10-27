-- Trace every authenticated ADMIN API request without retaining credentials or request bodies.
-- Apply using the project's migration procedure before deploying code that enables the filter.
CREATE TABLE admin_request_audit (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    actor_user_id BIGINT NOT NULL,
    http_method VARCHAR(10) NOT NULL,
    request_path VARCHAR(2048) NOT NULL,
    started_at DATETIME(6) NOT NULL,
    completed_at DATETIME(6) NULL,
    status_code INT NULL
) ENGINE=InnoDB;

CREATE INDEX idx_admin_request_actor_time ON admin_request_audit (actor_user_id, started_at);
