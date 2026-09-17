-- Corte diario global (daily_cash_cuts): pasa a ser la posición de las CUENTAS BANCARIAS.
--
-- Negocio definió el alcance de cada corte: "Cortes y saldos" es bancos y "Caja General" es
-- efectivo físico. De ahí se siguen tres cambios en la fórmula:
--
--   1. El efectivo sale del total. entradas_efectivo y retornos_efectivo se siguen calculando
--      y guardando como dato informativo del día, pero ya no suman al saldo bancario: ese
--      dinero entra y sale de Caja General y allí está su libro.
--   2. El retiro sin tarjeta entra al total. Sale de la cuenta origen de la parcialidad y
--      faltaba por completo: el corte sumaba transferencia, depósito, efectivo y cheque, pero
--      nunca este tipo, así que el saldo bancario quedaba inflado por cada retiro entregado.
--   3. El cheque cobrado se resta (columna agregada en 2026-09-17_add_salidas_cheque_cobrado_daily_cut.sql).
--
-- La columna nueva entra con DEFAULT 0. A diferencia de los cambios anteriores, este SÍ cambia
-- el significado de los cortes ya guardados, porque su fórmula era otra. Para dejar la cadena
-- de saldos coherente hay que recalcular la serie DESPUÉS de desplegar:
--
--   POST /api/daily-cash-cuts/recalculate?desde=YYYY-MM-DD   (rol ADMIN)
--
-- Ese recálculo es determinista e idempotente: rehace los importes desde los pagos, retornos
-- y cheques originales. Lo único que respeta es el saldo inicial capturado a mano en el primer
-- corte de la historia; si ese número incluía efectivo, hay que corregirlo aparte.
--
-- EJECUCIÓN ÚNICA. Hibernate (ddl-auto=update) también agrega la columna al arrancar.

ALTER TABLE daily_cash_cuts
  ADD COLUMN retornos_retiro_sin_tarjeta DECIMAL(15,2) NOT NULL DEFAULT 0 AFTER retornos_cheque;
