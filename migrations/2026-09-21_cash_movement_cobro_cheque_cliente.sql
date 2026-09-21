-- Permite registrar en Caja General el efectivo recibido al cobrar un cheque.
--
-- Algunas instalaciones crearon `cash_general_movements.tipo` como ENUM a
-- partir del atributo Java, aunque la migración base lo define como VARCHAR.
-- En esas bases el alta del movimiento falla con "Data truncated for column
-- 'tipo'" al intentar guardar COBRO_CHEQUE_CLIENTE.
--
-- EJECUCIÓN ÚNICA. No modifica movimientos ni saldos existentes.

ALTER TABLE cash_general_movements
  MODIFY COLUMN tipo ENUM(
    'COBRO_CHEQUE_CLIENTE',
    'EFECTIVO',
    'CHEQUE',
    'TRANSFERENCIA',
    'DEPOSITO',
    'RETIRO_CON_TARJETA',
    'RETIRO_SIN_TARJETA'
  ) NOT NULL;
