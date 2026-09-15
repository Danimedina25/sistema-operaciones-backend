-- Auditoría de eliminaciones administrativas de cortes de Caja General.
CREATE TABLE IF NOT EXISTS cash_general_deletion_audit (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    deleted_day_id BIGINT NOT NULL,
    fecha DATE NOT NULL,
    saldo_inicial DECIMAL(15,2) NOT NULL,
    saldo_esperado DECIMAL(15,2) NOT NULL,
    saldo_contado DECIMAL(15,2) NULL,
    movement_count INT NOT NULL,
    motivo VARCHAR(500) NOT NULL,
    deleted_by BIGINT NOT NULL,
    deleted_at DATETIME(6) NOT NULL,
    CONSTRAINT fk_cg_deletion_user FOREIGN KEY (deleted_by) REFERENCES users(id),
    INDEX idx_cg_deletion_fecha (fecha),
    INDEX idx_cg_deletion_deleted_at (deleted_at)
);
