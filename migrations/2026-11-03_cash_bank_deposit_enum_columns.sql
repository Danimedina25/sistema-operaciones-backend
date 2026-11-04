-- Apply after 2026-10-15_cheques_envios_a_cuentas.sql.
-- Match the enum columns expected by Hibernate before starting with ddl-auto=validate.
ALTER TABLE cash_bank_deposit_counts
  MODIFY COLUMN denomination ENUM(
    'D1000','D500','D200','D100','D50','D20','D10','D5','D2','D1','D050'
  ) NOT NULL;

ALTER TABLE cash_bank_deposit_return_counts
  MODIFY COLUMN denomination ENUM(
    'D1000','D500','D200','D100','D50','D20','D10','D5','D2','D1','D050'
  ) NOT NULL;

ALTER TABLE cash_bank_deposits
  MODIFY COLUMN origen ENUM('EFECTIVO_CAJA','CHEQUE') NOT NULL,
  MODIFY COLUMN estatus ENUM(
    'PENDIENTE','CONFIRMADO','RECHAZADO_PENDIENTE_DEVOLUCION','CANCELADO'
  ) NOT NULL;
