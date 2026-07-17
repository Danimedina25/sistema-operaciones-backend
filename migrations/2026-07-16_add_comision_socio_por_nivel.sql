-- ============================================================================
-- Migración: comisión de socio comercial independiente por nivel (1, 2, 3)
-- Fecha: 2026-07-16
-- Ejecutar a mano contra producción (sin Flyway/Liquibase en este proyecto).
-- DEBE correrse ANTES de desplegar el backend nuevo (para que Hibernate no
-- cree columnas nuevas huérfanas con ddl-auto=update al arrancar).
--
-- ANTES DE EJECUTAR: respaldo completo de la base de datos.
--   docker compose exec mysql mysqldump -u root -p db_sistema_operaciones \
--     > backup_pre_comision_por_nivel_$(date +%Y%m%d_%H%M%S).sql
-- ============================================================================

START TRANSACTION;

-- 1. Nuevas columnas NULLABLE (solo aplican si ese nivel está activo, mismo
--    criterio que socio_comercial_id_nivel_2/3, que tampoco son NOT NULL).
ALTER TABLE payment_operations
  ADD COLUMN porcentaje_comision_socio_nivel_2 DECIMAL(5,2) NULL AFTER porcentaje_comision_socio;

ALTER TABLE payment_operations
  ADD COLUMN porcentaje_comision_socio_nivel_3 DECIMAL(5,2) NULL AFTER porcentaje_comision_socio_nivel_2;

-- 2. Backfill histórico: SOLO para filas donde ese nivel ya tenía un socio
--    asignado (no se fabrica comisión donde no existía red comercial en ese
--    nivel). Se copia el valor histórico único de porcentaje_comision_socio
--    de esa misma fila, que es el mejor dato disponible (antes de este
--    cambio, ese valor se aplicaba por igual a todos los niveles).
UPDATE payment_operations
SET porcentaje_comision_socio_nivel_2 = porcentaje_comision_socio
WHERE socio_comercial_id_nivel_2 IS NOT NULL;

UPDATE payment_operations
SET porcentaje_comision_socio_nivel_3 = porcentaje_comision_socio
WHERE socio_comercial_id_nivel_3 IS NOT NULL;

COMMIT;

-- ============================================================================
-- Verificación (ejecutar después del COMMIT, antes de desplegar el backend):
--   DESCRIBE payment_operations;
--   SELECT COUNT(*) FROM payment_operations
--     WHERE socio_comercial_id_nivel_2 IS NOT NULL
--       AND porcentaje_comision_socio_nivel_2 IS NULL;               -- esperar 0
--   SELECT COUNT(*) FROM payment_operations
--     WHERE socio_comercial_id_nivel_3 IS NOT NULL
--       AND porcentaje_comision_socio_nivel_3 IS NULL;               -- esperar 0
--   SELECT COUNT(*) FROM payment_operations
--     WHERE socio_comercial_id_nivel_2 IS NULL
--       AND porcentaje_comision_socio_nivel_2 IS NOT NULL;           -- esperar 0
-- ============================================================================
