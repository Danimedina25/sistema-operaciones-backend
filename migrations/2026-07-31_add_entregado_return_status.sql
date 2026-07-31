-- ============================================================================
-- Migración: agrega ENTREGADO al enum de estatus de retornos
-- Fecha: 2026-07-31
-- Ejecutar a mano contra producción (sin Flyway/Liquibase en este proyecto).
--
-- A diferencia de otras migraciones (que solo agregaban columnas nuevas, algo
-- que Hibernate ya hace solo con ddl-auto=update), esta modifica la lista de
-- valores permitidos de una columna ENUM que ya existe. Eso Hibernate NO lo
-- hace automáticamente. El backend ya está desplegado con el valor ENTREGADO
-- en el código; sin correr esto, cualquier intento de guardar un retorno en
-- ese estatus falla con "Data truncated for column 'estatus'" (MySQL 1265).
--
-- ANTES DE EJECUTAR: respaldo completo de la base de datos.
--   docker compose exec mysql mysqldump -u root -p db_sistema_operaciones \
--     > backup_pre_entregado_return_status_$(date +%Y%m%d_%H%M%S).sql
-- ============================================================================

START TRANSACTION;

ALTER TABLE operation_return_payments
  MODIFY COLUMN estatus ENUM('SOLICITADO','EN_RECOLECCION','ENTREGADO','RETORNADO') NOT NULL;

COMMIT;

-- ============================================================================
-- Verificación (ejecutar después del COMMIT):
--   SHOW COLUMNS FROM operation_return_payments LIKE 'estatus';
--   -- el campo "Type" debe mostrar los 4 valores, incluyendo 'ENTREGADO'
--   SELECT estatus, COUNT(*) FROM operation_return_payments GROUP BY estatus;
--   -- confirmar que los retornos existentes conservan su estatus previo
-- ============================================================================
