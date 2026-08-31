-- ============================================================================
-- Migración: crea la tabla operation_return_installments (parcialidades de una
-- solicitud de retorno)
-- Fecha: 2026-09-01
-- Ejecutar a mano contra producción (sin Flyway/Liquibase en este proyecto).
--
-- Hibernate (ddl-auto=update) también crea esta tabla al iniciar el backend;
-- este script se provee para despliegues donde se prefiera migrar el esquema
-- antes de levantar la nueva versión. El esquema aquí debe coincidir con
-- OperationReturnInstallment.java para que ddl-auto=update no intente
-- modificarlo después.
--
-- ANTES DE EJECUTAR: respaldo completo de la base de datos.
--   docker compose exec mysql sh -c 'exec mysqldump -u"$MYSQL_USER" -p"$MYSQL_PASSWORD" "$MYSQL_DATABASE"' \
--     > backup_pre_return_installments_$(date +%Y%m%d_%H%M%S).sql
-- ============================================================================

START TRANSACTION;

CREATE TABLE IF NOT EXISTS operation_return_installments (
    id                        BIGINT        NOT NULL AUTO_INCREMENT,
    solicitud_id              BIGINT        NOT NULL,
    version                   BIGINT        NOT NULL DEFAULT 0,
    monto                     DECIMAL(15,2) NOT NULL,
    tipo_pago                 VARCHAR(30)   NOT NULL,
    estatus                   VARCHAR(20)   NOT NULL,
    cuenta_origen_id          BIGINT        NULL,
    comprobante_url           VARCHAR(500)  NULL,
    comprobante_entrega_url   VARCHAR(500)  NULL,
    codigo_retiro_sin_tarjeta VARCHAR(40)   NULL,
    fecha_hora_recoleccion    DATETIME      NULL,
    fecha_realizacion         DATETIME      NULL,
    fecha_entrega             DATETIME      NULL,
    fecha_confirmacion        DATETIME      NULL,
    fecha_cancelacion         DATETIME      NULL,
    observaciones             VARCHAR(500)  NULL,
    creado_por                BIGINT        NOT NULL,
    realizado_por             BIGINT        NULL,
    entregado_por             BIGINT        NULL,
    cancelado_por             BIGINT        NULL,
    created_at                DATETIME      NOT NULL,
    updated_at                DATETIME      NOT NULL,
    PRIMARY KEY (id),
    KEY idx_return_installment_solicitud (solicitud_id),
    KEY idx_return_installment_estatus (estatus),
    KEY idx_return_installment_realizacion (tipo_pago, estatus, fecha_realizacion),
    CONSTRAINT fk_return_installment_solicitud
        FOREIGN KEY (solicitud_id) REFERENCES operation_return_payments (id),
    CONSTRAINT fk_return_installment_cuenta_origen
        FOREIGN KEY (cuenta_origen_id) REFERENCES bank_accounts (id),
    CONSTRAINT fk_return_installment_creado_por
        FOREIGN KEY (creado_por) REFERENCES users (id),
    CONSTRAINT fk_return_installment_realizado_por
        FOREIGN KEY (realizado_por) REFERENCES users (id),
    CONSTRAINT fk_return_installment_entregado_por
        FOREIGN KEY (entregado_por) REFERENCES users (id),
    CONSTRAINT fk_return_installment_cancelado_por
        FOREIGN KEY (cancelado_por) REFERENCES users (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

COMMIT;

-- ============================================================================
-- Verificación:
--   SHOW CREATE TABLE operation_return_installments;
-- ============================================================================
