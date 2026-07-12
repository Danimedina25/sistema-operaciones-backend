-- ============================================================================
-- Migración: separar comisión de oficina y de socio comercial
-- Fecha: 2026-07-12
-- Ejecutar a mano contra producción (sin Flyway/Liquibase en este proyecto).
-- DEBE correrse ANTES de desplegar el backend nuevo (para que Hibernate no
-- cree una columna nueva huérfana con ddl-auto=update al arrancar).
--
-- ANTES DE EJECUTAR: respaldo completo de la base de datos.
--   docker exec <mysql_container> \
--     mysqldump -u <user> -p<password> <database> > backup_pre_comision_split_$(date +%Y%m%d_%H%M%S).sql
-- ============================================================================

START TRANSACTION;

-- 1. clientes: renombrar la columna existente (mismo dato, mismo tipo).
ALTER TABLE clientes
  CHANGE COLUMN porcentaje_comision_aplicado porcentaje_comision_socio DECIMAL(5,2) NOT NULL;

-- 2. clientes: nueva columna de comisión de oficina, con default seguro y
--    conocido (1.50) aplicado en la misma sentencia — sin ventana de NULL.
ALTER TABLE clientes
  ADD COLUMN porcentaje_comision_oficina DECIMAL(5,2) NOT NULL DEFAULT 1.50 AFTER porcentaje_comision_socio;

-- 3. payment_operations: renombrar la columna existente (mismo dato, mismo tipo).
ALTER TABLE payment_operations
  CHANGE COLUMN porcentaje_comision_aplicado porcentaje_comision_socio DECIMAL(5,2) NOT NULL;

-- 4. payment_operations.porcentaje_comision_oficina YA EXISTE, ya es NOT NULL
--    y ya está poblada en todas las filas históricas (estaba hardcodeada a
--    1.5 en el backend hasta ahora) — no requiere ningún cambio de esquema
--    ni de datos. No se fabrican valores históricos nuevos.

COMMIT;

-- ============================================================================
-- Verificación (ejecutar después del COMMIT, antes de desplegar el backend):
--   DESCRIBE clientes;
--   DESCRIBE payment_operations;
--   SELECT COUNT(*) FROM clientes WHERE porcentaje_comision_oficina IS NULL;        -- esperar 0
--   SELECT COUNT(*) FROM clientes WHERE porcentaje_comision_socio IS NULL;          -- esperar 0
--   SELECT COUNT(*) FROM payment_operations WHERE porcentaje_comision_socio IS NULL; -- esperar 0
-- ============================================================================
