-- ============================================================================
-- Migración: agrega EN_PROCESO al enum de estatus de comprobantes de pago
-- Fecha: 2026-08-30
-- Ejecutar a mano contra producción (sin Flyway/Liquibase en este proyecto).
--
-- Igual que 2026-07-31_add_entregado_return_status.sql: modifica la lista de
-- valores permitidos de una columna ENUM que ya existe. Hibernate
-- (ddl-auto=update) NO agrega valores nuevos a un ENUM existente. El backend
-- ya está desplegado con PaymentStatus.EN_PROCESO en el código; sin correr
-- esto, el primer PATCH /api/operations/payments/{id}/in-progress falla con
-- "Data truncated for column 'estatus'" (MySQL 1265).
--
-- Las columnas nuevas de OperationPayment (en_proceso_por, fecha_en_proceso)
-- SÍ las crea Hibernate solo al levantar el backend — no van en este script.
--
-- ANTES DE EJECUTAR: respaldo completo de la base de datos.
--   docker compose exec mysql sh -c 'exec mysqldump -u"$MYSQL_USER" -p"$MYSQL_PASSWORD" "$MYSQL_DATABASE"' \
--     > backup_pre_en_proceso_payment_status_$(date +%Y%m%d_%H%M%S).sql
--
-- ANTES DE EJECUTAR: confirmar el tipo actual de la columna.
--   SHOW COLUMNS FROM operation_payments LIKE 'estatus';
--   -- Si "Type" es enum('PENDIENTE_VALIDACION','VALIDADA','RECHAZADA'): correr el ALTER de abajo.
--   -- Si "Type" ya es varchar(...): NO hace falta este ALTER, el valor nuevo funciona solo.
-- ============================================================================

START TRANSACTION;

ALTER TABLE operation_payments
  MODIFY COLUMN estatus ENUM(
    'PENDIENTE_VALIDACION',
    'EN_PROCESO',
    'VALIDADA',
    'RECHAZADA'
  ) NOT NULL;

COMMIT;

-- ============================================================================
-- Verificación (ejecutar después del COMMIT):
--   SHOW COLUMNS FROM operation_payments LIKE 'estatus';
--   -- el campo "Type" debe mostrar los 4 valores, incluyendo 'EN_PROCESO'
--   SELECT estatus, COUNT(*) FROM operation_payments GROUP BY estatus;
--   -- confirmar que los comprobantes existentes conservan su estatus previo
-- ============================================================================
