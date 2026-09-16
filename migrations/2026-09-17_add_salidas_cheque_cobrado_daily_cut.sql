-- Corte diario global (daily_cash_cuts): nueva salida "cheque cobrado".
--
-- Se confirmó con negocio el alcance de cada corte: "Cortes y saldos" es la posición de las
-- CUENTAS BANCARIAS y "Caja General" es el efectivo físico. Bajo esa definición, cobrar un
-- cheque en ventanilla saca dinero de un banco y lo convierte en efectivo, así que es una
-- salida de este corte; su entrada correspondiente ya vive en el libro de Caja General.
--
-- Hasta ahora ese movimiento era invisible aquí: el corte global sólo sumaba pagos, retornos
-- y comisiones, y nunca miró cash_general_movements. El saldo bancario quedaba inflado.
--
-- Se agrega como columna con DEFAULT 0 y NO como recálculo: los cortes históricos no se
-- recalculan (para fecha < hoy manda la fila almacenada), y con valor cero toda fila previa
-- conserva idénticos total_salidas y saldo_final. La cadena de saldo inicial no se mueve.
--
-- EJECUCIÓN ÚNICA. Hibernate (ddl-auto=update) también agrega la columna al arrancar; este
-- script permite migrar antes del despliegue.

ALTER TABLE daily_cash_cuts
  ADD COLUMN salidas_cheque_cobrado DECIMAL(15,2) NOT NULL DEFAULT 0 AFTER total_retornos;
