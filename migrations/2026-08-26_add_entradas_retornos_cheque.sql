-- Los pagos y retornos tipo CHEQUE existian como PaymentType pero se excluian
-- del corte de caja y del saldo bancario por cuenta (solo se sumaban
-- TRANSFERENCIA, DEPOSITO y EFECTIVO). Se agregan columnas dedicadas para
-- que los cheques se contemplen igual que los demas medios de pago.
-- Hibernate (ddl-auto=update) tambien crea estas columnas al iniciar el
-- backend; este script queda disponible para despliegues donde se prefiera
-- migrar antes.
ALTER TABLE daily_cash_cuts
  ADD COLUMN entradas_cheque DECIMAL(15,2) NOT NULL DEFAULT 0 AFTER entradas_efectivo,
  ADD COLUMN retornos_cheque DECIMAL(15,2) NOT NULL DEFAULT 0 AFTER retornos_efectivo;

ALTER TABLE bank_account_daily_cuts
  ADD COLUMN entradas_cheque DECIMAL(15,2) NOT NULL DEFAULT 0 AFTER entradas_deposito;
