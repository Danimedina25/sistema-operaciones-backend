-- Caja General: el concepto capturable pasa de RETIRO_CON_TARJETA a RETIRO_SIN_TARJETA.
--
-- El retiro con tarjeta nunca existió en la operación: se retira sin tarjeta, con un código
-- generado contra una cuenta concreta. Eso elimina el motivo por el que ese concepto no se
-- contabilizaba —"no se sabe de qué cuenta sale"—: sí se sabe, y es obligatoria.
--
-- Queda entonces un solo hecho contable con dos instrumentos: efectivo que sale de una cuenta
-- bancaria y entra a la caja. Por eso las columnas de "salida por cheque" se generalizan a
-- "salida hacia Caja General" y cubren ambos.
--
-- Con esto desaparece el último uso del catálogo fijo de nombres de banco: todos los
-- movimientos que tocan un banco lo hacen ahora por llave foránea.
--
-- EJECUCIÓN ÚNICA. Hibernate (ddl-auto=update) NO renombra columnas: crearía las nuevas y
-- dejaría las viejas con los datos dentro, así que este script debe correr ANTES de desplegar.

-- 1) Generalizar las columnas de salida hacia Caja General -------------------
ALTER TABLE bank_account_daily_cuts
  CHANGE COLUMN salidas_cheque salidas_caja_general DECIMAL(15,2) NOT NULL DEFAULT 0;

ALTER TABLE daily_cash_cuts
  CHANGE COLUMN salidas_cheque_cobrado salidas_caja_general DECIMAL(15,2) NOT NULL DEFAULT 0;

-- 2) La cuenta vinculada ya no es exclusiva del cheque -----------------------
ALTER TABLE cash_general_movements
  DROP CHECK chk_cg_movement_bank_account;

ALTER TABLE cash_general_movements
  ADD CONSTRAINT chk_cg_movement_bank_account CHECK (
      bank_account_id IS NULL
   OR (tipo IN ('CHEQUE', 'RETIRO_SIN_TARJETA') AND direccion = 'ENTRADA')
  );

-- 3) Movimientos históricos capturados como retiro con tarjeta ---------------
-- Se reetiquetan al concepto real. Conservan su texto `banco` y su bank_account_id NULL:
-- no se infiere ninguna cuenta a partir del nombre, así que siguen fuera del saldo bancario.
UPDATE cash_general_movements
   SET tipo = 'RETIRO_SIN_TARJETA'
 WHERE tipo = 'RETIRO_CON_TARJETA';
