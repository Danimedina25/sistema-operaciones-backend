-- ============================================================================
-- Migración: agrega COMISIONES al enum de notificaciones (columna modulo)
-- Fecha: 2026-08-20
-- Ejecutar a mano contra producción (sin Flyway/Liquibase en este proyecto).
--
-- Mismo caso que 2026-07-31_add_entregado_return_status.sql y
-- 2026-08-20_add_notification_return_payment_types.sql: Hibernate
-- (ddl-auto=update) no agrega valores nuevos a un ENUM que ya existe.
-- NotificationModule.COMISIONES se agregó al código el 2026-08-17 (mismo
-- commit que CASH_RETURN_REQUESTED), pero la migración que se corrió para
-- ese commit solo cubrió las columnas 'tipo' y 'reference_type' — se
-- olvidó 'modulo'. Como resultado, CUALQUIER pago de comisión (individual
-- o por lote) falla con "Data truncated for column 'modulo'" (MySQL 1265)
-- al intentar notificar al beneficiario, y el usuario solo ve "Ocurrió un
-- error interno en el servidor".
--
-- ANTES DE EJECUTAR: respaldo completo de la base de datos.
--   docker compose exec mysql mysqldump -u root -p db_sistema_operaciones \
--     > backup_pre_notification_modulo_comisiones_$(date +%Y%m%d_%H%M%S).sql
--
-- ANTES DE EJECUTAR: confirmar los valores actuales (deben ser
-- 'OPERACIONES','PAGOS','SISTEMA' sin 'COMISIONES'; si no coincide, detente
-- y avisa).
--   SHOW COLUMNS FROM notifications LIKE 'modulo';
-- ============================================================================

START TRANSACTION;

ALTER TABLE notifications
  MODIFY COLUMN modulo ENUM(
    'OPERACIONES',
    'PAGOS',
    'COMISIONES',
    'SISTEMA'
  ) NOT NULL;

COMMIT;

-- ============================================================================
-- Verificación (ejecutar después del COMMIT):
--   SHOW COLUMNS FROM notifications LIKE 'modulo';
--   -- "Type" debe incluir los 4 valores
--   SELECT modulo, COUNT(*) FROM notifications GROUP BY modulo;
--   -- confirmar que las notificaciones existentes conservan su valor previo
-- ============================================================================
