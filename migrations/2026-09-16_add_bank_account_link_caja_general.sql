-- Caja General: el cheque cobrado saca dinero de una cuenta bancaria real.
--
-- Hasta ahora el movimiento sólo guardaba el nombre del banco como texto libre contra un
-- catálogo fijo de seis cadenas, sin ninguna relación con `bank_accounts`. El resultado es
-- que cobrar un cheque subía el efectivo de Caja General y el saldo bancario nunca bajaba.
--
-- Se agrega la relación real. Un mismo renglón de `cash_general_movements` representa a la
-- vez la entrada de efectivo y la salida bancaria: no existe una tabla de asientos porque el
-- libro bancario se deriva de las tres fuentes que ya tienen llave foránea a la cuenta
-- (pagos validados, parcialidades completadas y estos cheques). Así es imposible que exista
-- la entrada de efectivo sin su salida bancaria.
--
-- Movimientos históricos: conservan su texto `banco` como snapshot y quedan con
-- bank_account_id NULL, es decir "cuenta no vinculada". NO se infiere ninguna relación a
-- partir del nombre del banco, así que quedan fuera del libro bancario a propósito.
--
-- EJECUCIÓN ÚNICA. MySQL 8 no admite IF NOT EXISTS en ADD COLUMN / ADD CONSTRAINT /
-- CREATE INDEX, igual que en 2026-08-26_add_entradas_retornos_cheque.sql. Hibernate
-- (ddl-auto=update) también crea columnas, FK e índices al arrancar; este script existe
-- para migrar antes del despliegue y para traer además las restricciones CHECK, que
-- Hibernate no genera.

-- 1) Relación de Caja General con la cuenta bancaria -------------------------
ALTER TABLE cash_general_movements
  ADD COLUMN bank_account_id BIGINT NULL AFTER banco;

ALTER TABLE cash_general_movements
  ADD CONSTRAINT fk_cg_movement_bank_account
  FOREIGN KEY (bank_account_id) REFERENCES bank_accounts(id);

-- Sólo un cheque cobrado de ENTRADA puede llevar cuenta bancaria vinculada.
-- La obligatoriedad de la cuenta para cheques NUEVOS vive en el servicio: aquí no puede
-- exigirse retroactivamente, porque los cheques históricos tienen bank_account_id NULL.
ALTER TABLE cash_general_movements
  ADD CONSTRAINT chk_cg_movement_bank_account CHECK (
      bank_account_id IS NULL
   OR (tipo = 'CHEQUE' AND direccion = 'ENTRADA')
  );

-- 2) El corte bancario por cuenta contempla la salida por cheque ------------
-- Se agrega como columna con DEFAULT 0 y nunca como recálculo: los cortes históricos no se
-- recalculan (para fecha < hoy manda la fila almacenada), y con valor cero toda fila previa
-- conserva idénticos total_salidas y saldo_final. Antes de este cambio ningún cheque tenía
-- cuenta vinculada, así que cero es el valor correcto para todo el histórico.
ALTER TABLE bank_account_daily_cuts
  ADD COLUMN salidas_cheque DECIMAL(15,2) NOT NULL DEFAULT 0 AFTER salidas_retornos;

-- 3) Auditoría de la eliminación administrativa -----------------------------
-- Al borrar un día de Caja General con cheques cobrados hay que rehacer los cortes
-- bancarios ya registrados de esas cuentas. Se deja constancia de cuántos se regeneraron.
ALTER TABLE cash_general_deletion_audit
  ADD COLUMN cortes_bancarios_recalculados INT NOT NULL DEFAULT 0;

-- 4) Índices de apoyo del libro bancario derivado ---------------------------
CREATE INDEX idx_cg_movement_bank_ledger
  ON cash_general_movements (bank_account_id, tipo, direccion);

CREATE INDEX idx_op_payment_bank_ledger
  ON operation_payments (cuenta_destino_id, estatus, fecha_validacion);

CREATE INDEX idx_ori_bank_ledger
  ON operation_return_installments (cuenta_origen_id, estatus, fecha_realizacion);
