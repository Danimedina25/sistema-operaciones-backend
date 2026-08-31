-- ============================================================================
-- Migración: agrega los NotificationType de parcialidades de retorno y el
-- reference_type RETURN_INSTALLMENT a los enums de notificaciones
-- Fecha: 2026-09-01
-- Ejecutar a mano contra producción (sin Flyway/Liquibase en este proyecto).
--
-- Mismo caso que 2026-08-20_add_notification_return_payment_types.sql:
-- Hibernate (ddl-auto=update) no agrega valores nuevos a un ENUM existente.
-- El backend nuevo emite estos tipos al confirmar/realizar/cancelar
-- parcialidades; sin correr esto, la notificación falla con
-- "Data truncated for column 'tipo'" / "'reference_type'" (MySQL 1265).
--
-- ANTES DE EJECUTAR: respaldo completo de la base de datos.
--   docker compose exec mysql sh -c 'exec mysqldump -u"$MYSQL_USER" -p"$MYSQL_PASSWORD" "$MYSQL_DATABASE"' \
--     > backup_pre_notification_installment_types_$(date +%Y%m%d_%H%M%S).sql
--
-- ANTES DE EJECUTAR: confirmar los valores actuales.
--   SHOW COLUMNS FROM notifications LIKE 'tipo';
--   SHOW COLUMNS FROM notifications LIKE 'reference_type';
--   -- si alguna ya es varchar(...): NO hace falta el ALTER correspondiente.
-- ============================================================================

START TRANSACTION;

ALTER TABLE notifications
  MODIFY COLUMN tipo ENUM(
    'OPERATION_CREATED',
    'PAYMENT_SUBMITTED',
    'PAYMENT_VALIDATED',
    'PAYMENT_REJECTED',
    'OPERATION_STATUS_CHANGED',
    'COMMISSION_PAID',
    'CASH_RETURN_REQUESTED',
    'RETURN_INSTALLMENT_SCHEDULED',
    'RETURN_INSTALLMENT_CODE_AVAILABLE',
    'RETURN_INSTALLMENT_DELIVERED',
    'RETURN_INSTALLMENT_COMPLETED',
    'RETURN_INSTALLMENT_CANCELLED',
    'RETURN_REQUEST_COMPLETED',
    'SYSTEM_ALERT'
  ) NOT NULL;

ALTER TABLE notifications
  MODIFY COLUMN reference_type ENUM(
    'PAYMENT_OPERATION',
    'OPERATION_PAYMENT',
    'COMMISSION',
    'RETURN_PAYMENT',
    'RETURN_INSTALLMENT',
    'NONE'
  ) NOT NULL;

COMMIT;

-- ============================================================================
-- Verificación (ejecutar después del COMMIT):
--   SHOW COLUMNS FROM notifications LIKE 'tipo';
--   SHOW COLUMNS FROM notifications LIKE 'reference_type';
--   SELECT tipo, COUNT(*) FROM notifications GROUP BY tipo;
-- ============================================================================
