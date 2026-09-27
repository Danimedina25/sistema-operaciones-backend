-- Transferencias entre cuentas propias.
--
-- La Jefa de Cuentas registra en Cortes y saldos el dinero que mueve de una cuenta propia a
-- otra. Es una fuente más del libro bancario derivado (BankLedgerQuery): una sola fila produce
-- la salida de la cuenta origen y la entrada de la destino, así que no puede existir una sin la
-- otra. No toca Caja General y no entra al corte global (daily_cash_cuts), donde sería neutra.
--
-- Se fecha por created_at, es decir, siempre el día en que se registra: nunca modifica un
-- corte bancario ya registrado. El servicio además rechaza la transferencia si alguna de las
-- dos cuentas ya tiene el corte de hoy.
--
-- EJECUCIÓN ÚNICA, antes de desplegar. MySQL 8 no admite IF NOT EXISTS en ADD COLUMN.
-- Hibernate (ddl-auto=update) crearía tabla y columnas, pero no los CHECK ni los DEFAULT 0
-- que conservan intactos los cortes históricos.

-- 1) Fuente del libro -------------------------------------------------------
CREATE TABLE bank_transfers (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    request_id VARCHAR(36) NOT NULL,
    cuenta_origen_id BIGINT NOT NULL,
    cuenta_destino_id BIGINT NOT NULL,
    monto DECIMAL(15,2) NOT NULL,
    referencia VARCHAR(300) NULL,
    comprobante_url VARCHAR(500) NULL,
    registrado_por BIGINT NOT NULL,
    created_at DATETIME(6) NOT NULL,
    CONSTRAINT uk_bank_transfer_request UNIQUE (request_id),
    CONSTRAINT fk_bank_transfer_origen FOREIGN KEY (cuenta_origen_id) REFERENCES bank_accounts(id),
    CONSTRAINT fk_bank_transfer_destino FOREIGN KEY (cuenta_destino_id) REFERENCES bank_accounts(id),
    CONSTRAINT fk_bank_transfer_usuario FOREIGN KEY (registrado_por) REFERENCES users(id),
    CONSTRAINT chk_bank_transfer_cuentas CHECK (cuenta_origen_id <> cuenta_destino_id),
    CONSTRAINT chk_bank_transfer_monto CHECK (monto > 0)
) ENGINE=InnoDB;

CREATE INDEX idx_bank_transfer_origen ON bank_transfers (cuenta_origen_id, created_at);
CREATE INDEX idx_bank_transfer_destino ON bank_transfers (cuenta_destino_id, created_at);
CREATE INDEX idx_bank_transfer_fecha ON bank_transfers (created_at);

-- 2) Corte bancario por cuenta ----------------------------------------------
-- Columnas con DEFAULT 0 y sin recálculo: antes de este cambio no existía ninguna
-- transferencia interna, así que cero es el valor correcto para todo el histórico y cada
-- fila previa conserva idénticos total_entradas, total_salidas y saldo_final.
ALTER TABLE bank_account_daily_cuts
  ADD COLUMN entradas_transferencia_interna DECIMAL(15,2) NOT NULL DEFAULT 0,
  ADD COLUMN salidas_transferencia_interna DECIMAL(15,2) NOT NULL DEFAULT 0;
