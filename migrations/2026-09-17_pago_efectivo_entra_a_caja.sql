-- Caja General: el pago de ingreso en efectivo entra a la caja al validarse.
--
-- Hasta ahora un pago EFECTIVO validado no dejaba rastro en Caja General: el dinero entraba
-- físicamente al efectivo pero el libro no lo registraba, así que el saldo de la caja se
-- quedaba corto. Es el espejo del retorno en efectivo, que ya generaba su salida automática.
--
-- La columna es única: un pago validado produce una sola entrada de caja, y esa unicidad es
-- la que impide duplicarla. La idempotencia del reintento la sigue dando request_id, con un
-- identificador determinista por pago.
--
-- No se rellenan los pagos en efectivo ya validados: su entrada exigiría el desglose por
-- denominación del momento en que se recibió el dinero, que nadie capturó. Quedan sin
-- vincular y se reportan como pendiente.
--
-- EJECUCIÓN ÚNICA. Hibernate (ddl-auto=update) también agrega la columna y su índice único;
-- este script permite migrar antes del despliegue.

ALTER TABLE cash_general_movements
  ADD COLUMN operation_payment_id BIGINT NULL AFTER installment_id;

ALTER TABLE cash_general_movements
  ADD CONSTRAINT uk_cg_movement_payment UNIQUE (operation_payment_id);

ALTER TABLE cash_general_movements
  ADD CONSTRAINT fk_cg_movement_payment
  FOREIGN KEY (operation_payment_id) REFERENCES operation_payments(id);

-- Un movimiento nace de un pago o de una parcialidad, nunca de los dos.
ALTER TABLE cash_general_movements
  ADD CONSTRAINT chk_cg_movement_single_source CHECK (
      operation_payment_id IS NULL OR installment_id IS NULL
  );
