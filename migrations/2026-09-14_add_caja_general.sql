-- Caja General: caja física independiente del corte bancario existente.
-- Hibernate (ddl-auto=update) también crea las tablas; ejecutar antes del despliegue
-- para disponer además de las restricciones CHECK de integridad financiera.
-- No importa saldos ni movimientos históricos de Excel.
CREATE TABLE IF NOT EXISTS cash_general_register (id BIGINT NOT NULL PRIMARY KEY);
INSERT IGNORE INTO cash_general_register (id) VALUES (1);

CREATE TABLE IF NOT EXISTS cash_general_days (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    version BIGINT NOT NULL DEFAULT 0,
    fecha DATE NOT NULL UNIQUE,
    saldo_inicial DECIMAL(15,2) NOT NULL,
    saldo_actual DECIMAL(15,2) NOT NULL,
    saldo_contado DECIMAL(15,2) NULL,
    diferencia DECIMAL(15,2) NULL,
    abierto_por BIGINT NOT NULL,
    cerrado_por BIGINT NULL,
    observaciones_cierre VARCHAR(500) NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    closed_at DATETIME(6) NULL,
    FOREIGN KEY (abierto_por) REFERENCES users(id),
    FOREIGN KEY (cerrado_por) REFERENCES users(id),
    CONSTRAINT chk_cg_day_balance CHECK (saldo_inicial >= 0 AND saldo_actual >= 0 AND (saldo_contado IS NULL OR saldo_contado >= 0)),
    CONSTRAINT chk_cg_day_close CHECK ((closed_at IS NULL AND saldo_contado IS NULL AND diferencia IS NULL AND cerrado_por IS NULL)
        OR (closed_at IS NOT NULL AND saldo_contado IS NOT NULL AND diferencia IS NOT NULL AND cerrado_por IS NOT NULL AND diferencia = saldo_contado - saldo_actual))
);
CREATE TABLE IF NOT EXISTS cash_general_movements (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    day_id BIGINT NOT NULL,
    request_id VARCHAR(36) NOT NULL UNIQUE,
    direccion VARCHAR(10) NOT NULL,
    tipo VARCHAR(30) NOT NULL,
    concepto VARCHAR(300) NOT NULL,
    banco VARCHAR(50) NULL,
    monto_manual DECIMAL(15,2) NULL,
    installment_id BIGINT NULL UNIQUE,
    saldo_acumulado DECIMAL(15,2) NOT NULL,
    comprobante_url VARCHAR(500) NULL,
    creado_por BIGINT NOT NULL,
    created_at DATETIME(6) NOT NULL,
    FOREIGN KEY (creado_por) REFERENCES users(id),
    CONSTRAINT fk_cg_movement_day FOREIGN KEY (day_id) REFERENCES cash_general_days(id),
    CONSTRAINT fk_cg_movement_installment FOREIGN KEY (installment_id) REFERENCES operation_return_installments(id),
    CONSTRAINT chk_cg_movement_source CHECK ((installment_id IS NULL AND monto_manual IS NOT NULL AND monto_manual > 0)
        OR (installment_id IS NOT NULL AND monto_manual IS NULL AND direccion = 'SALIDA' AND tipo = 'EFECTIVO')),
    CONSTRAINT chk_cg_movement_balance CHECK (saldo_acumulado >= 0)
);
CREATE TABLE IF NOT EXISTS cash_general_opening_counts (
    day_id BIGINT NOT NULL, denomination VARCHAR(10) NOT NULL, quantity INT NOT NULL,
    PRIMARY KEY (day_id, denomination), FOREIGN KEY (day_id) REFERENCES cash_general_days(id), CHECK (quantity >= 0)
);
CREATE TABLE IF NOT EXISTS cash_general_closing_counts (
    day_id BIGINT NOT NULL, denomination VARCHAR(10) NOT NULL, quantity INT NOT NULL,
    PRIMARY KEY (day_id, denomination), FOREIGN KEY (day_id) REFERENCES cash_general_days(id), CHECK (quantity >= 0)
);
CREATE TABLE IF NOT EXISTS cash_general_movement_counts (
    movement_id BIGINT NOT NULL, denomination VARCHAR(10) NOT NULL, quantity INT NOT NULL,
    PRIMARY KEY (movement_id, denomination), FOREIGN KEY (movement_id) REFERENCES cash_general_movements(id), CHECK (quantity >= 0)
);
