-- Apply once, before deploying the cheque backend. No payment statuses, dates,
-- accounts or existing financial amounts are modified by this migration.
ALTER TABLE operation_payments
    ADD COLUMN numero_cheque VARCHAR(255) NULL,
    ADD COLUMN banco_emisor VARCHAR(255) NULL,
    ADD COLUMN emisor VARCHAR(255) NULL,
    ADD COLUMN beneficiario VARCHAR(255) NULL,
    ADD COLUMN cheque_estado VARCHAR(255) NULL,
    ADD COLUMN cheque_destino_cobro VARCHAR(255) NULL,
    ADD COLUMN cheque_fecha_cobro DATETIME(6) NULL,
    ADD COLUMN cheque_fecha_deposito DATETIME(6) NULL,
    ADD COLUMN cheque_version BIGINT NOT NULL DEFAULT 0,
    ADD INDEX idx_cheques_list (tipo_pago, cheque_estado, created_at, id),
    ADD CONSTRAINT chk_cheque_estado CHECK (cheque_estado IS NULL OR
        (tipo_pago = 'CHEQUE' AND cheque_estado IN ('POR_COBRAR','DEPOSITADO','PENDIENTE_COBRO_EFECTIVO','COBRADO','DEVUELTO','CANCELADO'))),
    ADD CONSTRAINT chk_cheque_destino CHECK (cheque_destino_cobro IS NULL OR cheque_destino_cobro IN ('EFECTIVO','CUENTA_BANCARIA')),
    ADD CONSTRAINT chk_cheque_cobrado CHECK (cheque_estado IS NULL OR cheque_estado <> 'COBRADO' OR
        (estatus = 'VALIDADA' AND cheque_fecha_cobro IS NOT NULL AND cheque_destino_cobro IS NOT NULL AND
         ((cheque_destino_cobro = 'EFECTIVO' AND cuenta_destino_id IS NULL) OR
          (cheque_destino_cobro = 'CUENTA_BANCARIA' AND cuenta_destino_id IS NOT NULL))));

-- The payment PK is also the cheque PK. It guarantees one cheque per payment
-- without an independently editable duplicate of the monetary amount.
CREATE TABLE cheque_audit (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    pago_id BIGINT NOT NULL,
    request_id VARCHAR(36) NULL,
    command_hash VARCHAR(64) NULL,
    accion VARCHAR(255) NOT NULL,
    fecha DATETIME(6) NOT NULL,
    registrado_en DATETIME(6) NOT NULL,
    usuario_id BIGINT NOT NULL,
    usuario_nombre VARCHAR(255) NOT NULL,
    motivo VARCHAR(500) NULL,
    comprobante_url VARCHAR(500) NULL,
    resultado LONGTEXT NULL,
    detalle LONGTEXT NULL,
    CONSTRAINT uk_cheque_request UNIQUE (request_id),
    CONSTRAINT fk_cheque_audit_pago FOREIGN KEY (pago_id) REFERENCES operation_payments(id),
    CONSTRAINT fk_cheque_audit_usuario FOREIGN KEY (usuario_id) REFERENCES users(id),
    INDEX idx_cheque_audit_pago (pago_id, id)
) ENGINE=InnoDB;

-- cash_general_movements.tipo is VARCHAR(30) in the baseline migrations.
-- COBRO_CHEQUE_CLIENTE uses its existing pago_id uniqueness and is not a manual
-- bank withdrawal. Do not add it to CHEQUE/RETIRO_SIN_TARJETA ledger predicates.

-- Inventory only: all historical cheques deliberately retain NULL state.
SELECT id, operacion_id, estatus, monto, cuenta_destino_id, fecha_validacion
FROM operation_payments WHERE tipo_pago = 'CHEQUE' AND cheque_estado IS NULL ORDER BY id;
