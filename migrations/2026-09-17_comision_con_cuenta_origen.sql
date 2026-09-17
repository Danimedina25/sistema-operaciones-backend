-- Comisiones a socios comerciales: se registra desde qué cuenta se transfieren.
--
-- Todas las comisiones se pagan por transferencia, pero la tabla no guardaba de qué cuenta
-- salía el dinero. Por eso el corte POR CUENTA tenía salidas_comisiones fijo en cero mientras
-- el corte GLOBAL sí las restaba: la suma de los saldos por cuenta no cuadraba con el saldo
-- global, y la diferencia era exactamente el total de comisiones pagadas.
--
-- Con la cuenta de origen, la comisión pagada entra al libro bancario como salida y el corte
-- por cuenta la descuenta. La cuenta es obligatoria al marcar pagada, igual que en pagos y
-- retornos.
--
-- Las comisiones ya pagadas conservan cuenta_origen_id NULL: nadie registró de dónde salieron
-- y no se puede inferir. Quedan fuera del corte por cuenta —igual que los cheques históricos—
-- pero el corte global las sigue contando, porque a él sólo le importa que salieron de algún
-- banco.
--
-- EJECUCIÓN ÚNICA. Hibernate (ddl-auto=update) también agrega la columna y su llave foránea;
-- este script permite migrar antes del despliegue.

ALTER TABLE commercial_partner_commissions
  ADD COLUMN cuenta_origen_id BIGINT NULL AFTER payment_proof_url;

ALTER TABLE commercial_partner_commissions
  ADD CONSTRAINT fk_cpc_cuenta_origen
  FOREIGN KEY (cuenta_origen_id) REFERENCES bank_accounts(id);

CREATE INDEX idx_cpc_cuenta_origen_pago
  ON commercial_partner_commissions (cuenta_origen_id, status, paid_at);
