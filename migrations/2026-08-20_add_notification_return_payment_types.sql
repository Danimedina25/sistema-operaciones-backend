-- ============================================================================
-- Migración: agrega CASH_RETURN_REQUESTED y RETURN_PAYMENT a los enums de
-- notificaciones (tipo y reference_type)
-- Fecha: 2026-08-20
-- Ejecutar a mano contra producción (sin Flyway/Liquibase en este proyecto).
--
-- Mismo caso que 2026-07-31_add_entregado_return_status.sql: Hibernate
-- (ddl-auto=update) no agrega valores nuevos a un ENUM que ya existe. El
-- backend ya está desplegado con NotificationType.CASH_RETURN_REQUESTED y
-- NotificationReferenceType.RETURN_PAYMENT en el código (agregados para
-- notificar retornos en efectivo solicitados); sin correr esto, cualquier
-- solicitud de retorno que incluya un pago en efectivo o retiro sin tarjeta
-- falla con "Data truncated for column 'reference_type'" (MySQL 1265) al
-- intentar notificar a JEFA_CAJAS/ADMIN.
--
-- ANTES DE EJECUTAR: respaldo completo de la base de datos.
--   docker compose exec mysql mysqldump -u root -p db_sistema_operaciones \
--     > backup_pre_notification_return_payment_types_$(date +%Y%m%d_%H%M%S).sql
--
-- ANTES DE EJECUTAR: confirmar los valores actuales (deben coincidir con la
-- lista "antes" de cada ALTER de abajo; si no coinciden, detente y avisa).
--   SHOW COLUMNS FROM notifications LIKE 'tipo';
--   SHOW COLUMNS FROM notifications LIKE 'reference_type';
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
    'SYSTEM_ALERT'
  ) NOT NULL;

ALTER TABLE notifications
  MODIFY COLUMN reference_type ENUM(
    'PAYMENT_OPERATION',
    'OPERATION_PAYMENT',
    'COMMISSION',
    'RETURN_PAYMENT',
    'NONE'
  ) NOT NULL;

COMMIT;

-- ============================================================================
-- Verificación (ejecutar después del COMMIT):
--   SHOW COLUMNS FROM notifications LIKE 'tipo';
--   SHOW COLUMNS FROM notifications LIKE 'reference_type';
--   -- ambos "Type" deben incluir los 8 y 5 valores respectivamente
--   SELECT tipo, COUNT(*) FROM notifications GROUP BY tipo;
--   SELECT reference_type, COUNT(*) FROM notifications GROUP BY reference_type;
--   -- confirmar que las notificaciones existentes conservan su valor previo
-- ============================================================================
