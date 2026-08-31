-- ============================================================================
-- Migración de datos: backfill de parcialidades para los retornos históricos
-- Fecha: 2026-09-01
-- Ejecutar a mano contra producción (sin Flyway/Liquibase en este proyecto).
--
-- Por cada solicitud de retorno (operation_return_payments) que ya tuvo
-- actividad de pago (estatus <> 'SOLICITADO') y que aún no tiene parcialidad,
-- crea UNA parcialidad que arrastra los datos de pago de la solicitud. Con
-- esto los retornos completados históricos siguen apareciendo correctamente y
-- las sumas de corte de caja / saldos bancarios (que ahora leen
-- operation_return_installments) reproducen exactamente los totales previos.
--
-- Mapeo de estatus:
--   RETORNADO      -> COMPLETADA
--   ENTREGADO      -> ENTREGADA
--   EN_RECOLECCION -> PROGRAMADA
--
-- IDEMPOTENTE: el WHERE NOT EXISTS evita duplicar si se corre dos veces.
--
-- CORRER DESPUÉS de:
--   2026-09-01_create_operation_return_installments.sql
--   2026-09-01_add_parcialmente_retornado_return_status.sql
-- y ANTES de desplegar el backend nuevo.
--
-- ANTES DE EJECUTAR: respaldo completo de la base de datos.
--   docker compose exec mysql sh -c 'exec mysqldump -u"$MYSQL_USER" -p"$MYSQL_PASSWORD" "$MYSQL_DATABASE"' \
--     > backup_pre_backfill_return_installments_$(date +%Y%m%d_%H%M%S).sql
-- ============================================================================

START TRANSACTION;

INSERT INTO operation_return_installments (
    solicitud_id, version, monto, tipo_pago, estatus,
    cuenta_origen_id, comprobante_url, comprobante_entrega_url,
    codigo_retiro_sin_tarjeta, fecha_hora_recoleccion, fecha_realizacion,
    fecha_entrega, fecha_confirmacion, fecha_cancelacion, observaciones,
    creado_por, realizado_por, entregado_por, cancelado_por,
    created_at, updated_at
)
SELECT
    p.id,
    0,
    p.monto,
    p.tipo_pago,
    CASE p.estatus
        WHEN 'RETORNADO'      THEN 'COMPLETADA'
        WHEN 'ENTREGADO'      THEN 'ENTREGADA'
        WHEN 'EN_RECOLECCION' THEN 'PROGRAMADA'
        ELSE 'PROGRAMADA'
    END,
    p.cuenta_origen_id,
    p.comprobante_url,
    p.comprobante_entrega_efectivo_url,
    p.codigo_retiro_sin_tarjeta,
    p.fecha_hora_recoleccion_efectivo,
    CASE WHEN p.estatus = 'RETORNADO' THEN COALESCE(p.fecha_pago, p.fecha_entrega, p.updated_at) END,
    p.fecha_entrega,
    p.fecha_confirmacion_recoleccion,
    NULL,
    p.observaciones,
    COALESCE(p.pagado_por, p.solicitado_por),
    CASE WHEN p.estatus = 'RETORNADO' THEN COALESCE(p.pagado_por, p.solicitado_por) END,
    p.entregado_por,
    NULL,
    COALESCE(p.created_at, NOW()),
    COALESCE(p.updated_at, NOW())
FROM operation_return_payments p
WHERE p.estatus <> 'SOLICITADO'
  AND NOT EXISTS (
      SELECT 1 FROM operation_return_installments i WHERE i.solicitud_id = p.id
  );

COMMIT;

-- ============================================================================
-- Verificación (ejecutar después del COMMIT):
--   SELECT p.estatus AS solicitud_estatus, i.estatus AS parcialidad_estatus, COUNT(*)
--   FROM operation_return_payments p
--   LEFT JOIN operation_return_installments i ON i.solicitud_id = p.id
--   GROUP BY p.estatus, i.estatus;
--   -- toda solicitud <> SOLICITADO debe tener exactamente una parcialidad
--
--   -- Los totales de retorno del corte de caja no deben cambiar: comparar
--   -- SUM(monto) por tipo/fecha entre operation_return_payments (RETORNADO,
--   -- fecha_pago) y operation_return_installments (COMPLETADA, fecha_realizacion).
-- ============================================================================
