-- ============================================================================
-- Migración: agrega PARCIALMENTE_RETORNADO al enum de estatus de solicitudes
-- de retorno
-- Fecha: 2026-09-01
-- Ejecutar a mano contra producción (sin Flyway/Liquibase en este proyecto).
--
-- Igual que 2026-07-31_add_entregado_return_status.sql: Hibernate
-- (ddl-auto=update) NO agrega valores nuevos a una columna ENUM existente. El
-- backend nuevo recalcula solicitud.estatus a PARCIALMENTE_RETORNADO cuando
-- una solicitud tiene al menos una parcialidad completada y todavía saldo
-- pendiente; sin correr esto, ese guardado falla con
-- "Data truncated for column 'estatus'" (MySQL 1265).
--
-- ANTES DE EJECUTAR: respaldo completo de la base de datos.
--   docker compose exec mysql sh -c 'exec mysqldump -u"$MYSQL_USER" -p"$MYSQL_PASSWORD" "$MYSQL_DATABASE"' \
--     > backup_pre_parcialmente_retornado_$(date +%Y%m%d_%H%M%S).sql
--
-- ANTES DE EJECUTAR: confirmar el tipo actual de la columna.
--   SHOW COLUMNS FROM operation_return_payments LIKE 'estatus';
--   -- si "Type" ya es varchar(...): NO hace falta este ALTER.
-- ============================================================================

START TRANSACTION;

ALTER TABLE operation_return_payments
  MODIFY COLUMN estatus ENUM(
    'SOLICITADO',
    'EN_RECOLECCION',
    'ENTREGADO',
    'PARCIALMENTE_RETORNADO',
    'RETORNADO'
  ) NOT NULL;

COMMIT;

-- ============================================================================
-- Verificación (ejecutar después del COMMIT):
--   SHOW COLUMNS FROM operation_return_payments LIKE 'estatus';
--   SELECT estatus, COUNT(*) FROM operation_return_payments GROUP BY estatus;
--   -- confirmar que las solicitudes existentes conservan su estatus previo
-- ============================================================================
