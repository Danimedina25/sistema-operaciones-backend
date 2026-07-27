-- ============================================================================
-- Migración: porcentaje de comisión propio de cada socio comercial
-- Fecha: 2026-07-19
-- Ejecutar a mano contra producción (sin Flyway/Liquibase en este proyecto).
-- DEBE correrse ANTES de desplegar el backend nuevo (para que Hibernate no
-- cree columnas nuevas huérfanas con ddl-auto=update al arrancar).
--
-- ANTES DE EJECUTAR: respaldo completo de la base de datos.
--   docker compose exec mysql mysqldump -u root -p db_sistema_operaciones \
--     > backup_pre_porcentaje_comision_socios_$(date +%Y%m%d_%H%M%S).sql
-- ============================================================================

START TRANSACTION;

-- 1. Socios comerciales nivel 2/3 (tabla commercial_partners).
ALTER TABLE commercial_partners
  ADD COLUMN porcentaje_comision DECIMAL(5,2) NOT NULL DEFAULT 0.00;

-- 2. Socio comercial nivel 1 (User con configuración en commercial_partner_settings).
ALTER TABLE commercial_partner_settings
  ADD COLUMN porcentaje_comision DECIMAL(5,2) NOT NULL DEFAULT 0.00;

COMMIT;

-- ============================================================================
-- Verificación (ejecutar después del COMMIT, antes de desplegar el backend):
--   DESCRIBE commercial_partners;
--   DESCRIBE commercial_partner_settings;
--   SELECT COUNT(*) FROM commercial_partners WHERE porcentaje_comision IS NULL;          -- esperar 0
--   SELECT COUNT(*) FROM commercial_partner_settings WHERE porcentaje_comision IS NULL;  -- esperar 0
-- ============================================================================
