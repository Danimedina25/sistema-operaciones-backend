-- Retiros de efectivo de las cuentas hacia Caja General.
--
-- La Jefa de Cuentas registra el efectivo que sale de una cuenta (retiro con tarjeta, sin
-- tarjeta o cobro de cheque) con destino a Caja General. Queda PENDIENTE —"en tránsito"— hasta
-- que la Jefa de Cajas lo confirma con su desglose. Sólo al confirmar se crea la fila de
-- cash_general_movements, que es a la vez la entrada de efectivo y la salida bancaria; mientras
-- está pendiente no toca ningún saldo, pero sí reduce el disponible de la cuenta.
--
-- Caja General deja de capturar a mano cheques y retiros: esos entran sólo por este flujo.
-- RETIRO_CON_TARJETA vuelve a ser un concepto válido, siempre con cuenta vinculada.
--
-- EJECUCIÓN ÚNICA, antes de desplegar. MySQL 8 no admite IF NOT EXISTS en ADD COLUMN / ADD
-- CONSTRAINT. Hibernate (ddl-auto=update) crea tabla y columnas pero no los CHECK ni agrega
-- valores a los ENUM existentes.

START TRANSACTION;

-- 1) Retiros ----------------------------------------------------------------
CREATE TABLE bank_cash_withdrawals (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    version BIGINT NOT NULL DEFAULT 0,
    request_id VARCHAR(36) NOT NULL,
    bank_account_id BIGINT NOT NULL,
    banco_snapshot VARCHAR(100) NOT NULL,
    forma VARCHAR(30) NOT NULL,
    monto DECIMAL(15,2) NOT NULL,
    referencia VARCHAR(300) NULL,
    comprobante_url VARCHAR(500) NULL,
    estatus VARCHAR(20) NOT NULL,
    motivo VARCHAR(500) NULL,
    registrado_por BIGINT NOT NULL,
    registrado_en DATETIME(6) NOT NULL,
    resuelto_por BIGINT NULL,
    resuelto_en DATETIME(6) NULL,
    cash_movement_id BIGINT NULL,
    CONSTRAINT uk_bcw_request UNIQUE (request_id),
    CONSTRAINT uk_bcw_cash_movement UNIQUE (cash_movement_id),
    CONSTRAINT fk_bcw_account FOREIGN KEY (bank_account_id) REFERENCES bank_accounts(id),
    CONSTRAINT fk_bcw_registrado FOREIGN KEY (registrado_por) REFERENCES users(id),
    CONSTRAINT fk_bcw_resuelto FOREIGN KEY (resuelto_por) REFERENCES users(id),
    CONSTRAINT fk_bcw_cash_movement FOREIGN KEY (cash_movement_id) REFERENCES cash_general_movements(id),
    CONSTRAINT chk_bcw_monto CHECK (monto > 0),
    CONSTRAINT chk_bcw_forma CHECK (forma IN ('RETIRO_CON_TARJETA', 'RETIRO_SIN_TARJETA', 'COBRO_CHEQUE')),
    CONSTRAINT chk_bcw_estatus CHECK (estatus IN ('PENDIENTE', 'CONFIRMADO', 'RECHAZADO', 'CANCELADO')),
    -- Confirmado si y sólo si ya entró a la caja.
    CONSTRAINT chk_bcw_confirmado CHECK ((estatus = 'CONFIRMADO') = (cash_movement_id IS NOT NULL))
) ENGINE=InnoDB;

CREATE INDEX idx_bcw_estatus ON bank_cash_withdrawals (estatus, registrado_en);
CREATE INDEX idx_bcw_account_estatus ON bank_cash_withdrawals (bank_account_id, estatus);
CREATE INDEX idx_bcw_registrado_en ON bank_cash_withdrawals (registrado_en);

-- 2) Foto del "Retiro N de M" en el movimiento de caja -----------------------
ALTER TABLE cash_general_movements
  ADD COLUMN grupo_banco VARCHAR(60) NULL,
  ADD COLUMN total_cuentas_banco INT NULL;

-- 3) El retiro con tarjeta vuelve a llevar cuenta vinculada -------------------
ALTER TABLE cash_general_movements
  DROP CHECK chk_cg_movement_bank_account;

ALTER TABLE cash_general_movements
  ADD CONSTRAINT chk_cg_movement_bank_account CHECK (
      bank_account_id IS NULL
   OR (tipo IN ('CHEQUE', 'RETIRO_SIN_TARJETA', 'RETIRO_CON_TARJETA') AND direccion = 'ENTRADA')
  );

-- 4) Notificaciones ---------------------------------------------------------
-- Producción conserva NotificationType y NotificationReferenceType como ENUM.
ALTER TABLE notifications
  MODIFY COLUMN tipo ENUM(
    'OPERATION_CREATED',
    'PAYMENT_SUBMITTED',
    'PAYMENT_TYPE_CHANGED',
    'PAYMENT_VALIDATED',
    'PAYMENT_REJECTED',
    'OPERATION_STATUS_CHANGED',
    'COMMISSION_PAID',
    'CASH_RETURN_REQUESTED',
    'CHEQUE_CASH_COLLECTION_ASSIGNED',
    'CHEQUE_CASH_COLLECTION_RETURNED',
    'CHEQUE_CASH_COLLECTION_WITHDRAWN',
    'CHEQUE_CASH_COLLECTION_COMPLETED',
    'BANK_WITHDRAWAL_PENDING',
    'BANK_WITHDRAWAL_CONFIRMED',
    'BANK_WITHDRAWAL_REJECTED',
    'BANK_WITHDRAWAL_CANCELLED',
    'RETURN_INSTALLMENT_SCHEDULED',
    'RETURN_INSTALLMENT_CODE_AVAILABLE',
    'RETURN_INSTALLMENT_DELIVERED',
    'RETURN_INSTALLMENT_COMPLETED',
    'RETURN_INSTALLMENT_CANCELLED',
    'RETURN_REQUEST_COMPLETED',
    'SYSTEM_ALERT'
  ) NOT NULL;

ALTER TABLE notifications
  MODIFY COLUMN reference_type ENUM(
    'PAYMENT_OPERATION',
    'OPERATION_PAYMENT',
    'COMMISSION',
    'RETURN_PAYMENT',
    'RETURN_INSTALLMENT',
    'BANK_CASH_WITHDRAWAL',
    'NONE'
  ) NOT NULL;

COMMIT;
