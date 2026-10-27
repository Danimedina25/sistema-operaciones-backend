-- Vincula cheques a la misma bandeja de depósitos sin crear movimientos de efectivo.
-- Aplicar después de 2026-10-15_envios_caja_a_cuentas.sql.
ALTER TABLE cash_bank_deposits
  ADD COLUMN cheque_payment_id BIGINT NULL,
  ADD CONSTRAINT fk_cbd_cheque_payment FOREIGN KEY (cheque_payment_id) REFERENCES operation_payments(id);
CREATE INDEX idx_cbd_cheque_payment ON cash_bank_deposits (cheque_payment_id, estatus);
ALTER TABLE cash_bank_deposits DROP CHECK chk_cbd_confirmed;
ALTER TABLE cash_bank_deposits
  ADD CONSTRAINT chk_cbd_confirmed CHECK (
    (origen = 'EFECTIVO_CAJA' AND cheque_payment_id IS NULL
      AND ((estatus = 'CONFIRMADO') = (cash_movement_id IS NOT NULL)))
    OR (origen = 'CHEQUE' AND cheque_payment_id IS NOT NULL AND cash_movement_id IS NULL)
  );
