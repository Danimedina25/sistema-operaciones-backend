-- Append-only business audit for mutations. Callers provide an explicit safe snapshot;
-- credentials, tokens and full account numbers must never be persisted here.
CREATE TABLE business_audit_log (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    actor_user_id BIGINT NOT NULL,
    action_name VARCHAR(80) NOT NULL,
    entity_type VARCHAR(80) NOT NULL,
    entity_id BIGINT NOT NULL,
    before_json LONGTEXT NULL,
    after_json LONGTEXT NULL,
    occurred_at DATETIME(6) NOT NULL
) ENGINE=InnoDB;

CREATE INDEX idx_business_audit_entity ON business_audit_log (entity_type, entity_id, occurred_at);
CREATE INDEX idx_business_audit_actor ON business_audit_log (actor_user_id, occurred_at);
