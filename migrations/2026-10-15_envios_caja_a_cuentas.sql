-- Caja General → cuentas propias. Aplicar una vez antes de desplegar el flujo.
-- El envío pendiente reserva efectivo, sin alterar saldos. Al confirmar, una fila de
-- cash_general_movements es salida de Caja y entrada del libro bancario derivado.
-- MySQL 8: ALTER TABLE no admite IF NOT EXISTS en ADD CONSTRAINT.

CREATE TABLE cash_bank_deposits (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    version BIGINT NOT NULL DEFAULT 0,
    request_id VARCHAR(36) NOT NULL,
    origen VARCHAR(30) NOT NULL,
    bank_account_id BIGINT NOT NULL,
    monto DECIMAL(15,2) NOT NULL,
    forma VARCHAR(40) NOT NULL,
    referencia VARCHAR(300) NULL,
    comprobante_salida_url VARCHAR(500) NOT NULL,
    comprobante_banco_url VARCHAR(500) NULL,
    estatus VARCHAR(40) NOT NULL,
    motivo VARCHAR(500) NULL,
    registrado_por BIGINT NOT NULL,
    registrado_en DATETIME(6) NOT NULL,
    resuelto_por BIGINT NULL,
    resuelto_en DATETIME(6) NULL,
    devuelto_por BIGINT NULL,
    devuelto_en DATETIME(6) NULL,
    cash_movement_id BIGINT NULL,
    CONSTRAINT uk_cbd_request UNIQUE (request_id),
    CONSTRAINT uk_cbd_movement UNIQUE (cash_movement_id),
    CONSTRAINT fk_cbd_account FOREIGN KEY (bank_account_id) REFERENCES bank_accounts(id),
    CONSTRAINT fk_cbd_registered FOREIGN KEY (registrado_por) REFERENCES users(id),
    CONSTRAINT fk_cbd_resolved FOREIGN KEY (resuelto_por) REFERENCES users(id),
    CONSTRAINT fk_cbd_returned FOREIGN KEY (devuelto_por) REFERENCES users(id),
    CONSTRAINT fk_cbd_movement FOREIGN KEY (cash_movement_id) REFERENCES cash_general_movements(id),
    CONSTRAINT chk_cbd_amount CHECK (monto > 0),
    CONSTRAINT chk_cbd_origin CHECK (origen IN ('EFECTIVO_CAJA', 'CHEQUE')),
    CONSTRAINT chk_cbd_status CHECK (estatus IN ('PENDIENTE', 'CONFIRMADO', 'RECHAZADO_PENDIENTE_DEVOLUCION', 'CANCELADO')),
    CONSTRAINT chk_cbd_confirmed CHECK ((estatus = 'CONFIRMADO') = (cash_movement_id IS NOT NULL))
) ENGINE=InnoDB;

CREATE TABLE cash_bank_deposit_counts (
    deposit_id BIGINT NOT NULL,
    denomination VARCHAR(10) NOT NULL,
    quantity INT NOT NULL,
    PRIMARY KEY (deposit_id, denomination),
    CONSTRAINT fk_cbd_counts FOREIGN KEY (deposit_id) REFERENCES cash_bank_deposits(id),
    CONSTRAINT chk_cbd_count CHECK (quantity >= 0)
) ENGINE=InnoDB;

CREATE TABLE cash_bank_deposit_return_counts (
    deposit_id BIGINT NOT NULL,
    denomination VARCHAR(10) NOT NULL,
    quantity INT NOT NULL,
    PRIMARY KEY (deposit_id, denomination),
    CONSTRAINT fk_cbd_return_counts FOREIGN KEY (deposit_id) REFERENCES cash_bank_deposits(id),
    CONSTRAINT chk_cbd_return_count CHECK (quantity >= 0)
) ENGINE=InnoDB;

CREATE INDEX idx_cbd_status ON cash_bank_deposits (estatus, registrado_en);
CREATE INDEX idx_cbd_account_status ON cash_bank_deposits (bank_account_id, estatus);
CREATE INDEX idx_cbd_registered ON cash_bank_deposits (registrado_en);

ALTER TABLE cash_general_movements MODIFY COLUMN tipo ENUM(
    'COBRO_CHEQUE_CLIENTE', 'EFECTIVO', 'CHEQUE', 'TRANSFERENCIA',
    'DEPOSITO', 'RETIRO_CON_TARJETA', 'RETIRO_SIN_TARJETA',
    'ENVIO_A_CUENTA'
) NOT NULL;

ALTER TABLE cash_general_movements DROP CHECK chk_cg_movement_bank_account;
ALTER TABLE cash_general_movements
  ADD CONSTRAINT chk_cg_movement_bank_account CHECK (
      bank_account_id IS NULL
      OR (tipo IN ('CHEQUE', 'RETIRO_SIN_TARJETA', 'RETIRO_CON_TARJETA') AND direccion = 'ENTRADA')
      OR (tipo = 'ENVIO_A_CUENTA' AND direccion = 'SALIDA')
  );

ALTER TABLE notifications MODIFY COLUMN tipo ENUM(
    'OPERATION_CREATED', 'PAYMENT_SUBMITTED', 'PAYMENT_TYPE_CHANGED',
    'PAYMENT_VALIDATED', 'PAYMENT_REJECTED', 'OPERATION_STATUS_CHANGED',
    'COMMISSION_PAID', 'CASH_RETURN_REQUESTED',
    'CHEQUE_CASH_COLLECTION_ASSIGNED', 'CHEQUE_CASH_COLLECTION_RETURNED',
    'CHEQUE_CASH_COLLECTION_WITHDRAWN', 'CHEQUE_CASH_COLLECTION_COMPLETED',
    'BANK_WITHDRAWAL_PENDING', 'BANK_WITHDRAWAL_CONFIRMED',
    'BANK_WITHDRAWAL_REJECTED', 'BANK_WITHDRAWAL_CANCELLED',
    'CASH_BANK_DEPOSIT_PENDING', 'CASH_BANK_DEPOSIT_CONFIRMED',
    'CASH_BANK_DEPOSIT_REJECTED', 'CASH_BANK_DEPOSIT_RETURNED',
    'RETURN_INSTALLMENT_SCHEDULED', 'RETURN_INSTALLMENT_CODE_AVAILABLE',
    'RETURN_INSTALLMENT_DELIVERED', 'RETURN_INSTALLMENT_COMPLETED',
    'RETURN_INSTALLMENT_CANCELLED', 'RETURN_REQUEST_COMPLETED', 'SYSTEM_ALERT'
) NOT NULL;

ALTER TABLE notifications MODIFY COLUMN reference_type ENUM(
    'PAYMENT_OPERATION', 'OPERATION_PAYMENT', 'COMMISSION', 'RETURN_PAYMENT',
    'RETURN_INSTALLMENT', 'BANK_CASH_WITHDRAWAL', 'CASH_BANK_DEPOSIT', 'NONE'
) NOT NULL;
