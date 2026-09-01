-- Marca si la persona que recibió el efectivo (persona_que_recibio_efectivo) es
-- un autorizado registrado en la solicitud o alguien ajeno a la lista.
--
--   1    -> coincide con un autorizado (autorizado_para_recibir_efectivo_1..3)
--   0    -> recibió alguien fuera de la lista (excepción registrada a propósito)
--   NULL -> parcialidad histórica / no aplica
--
-- Al cerrar una entrega de efectivo / retiro sin tarjeta el frontend ahora
-- permite capturar el nombre de una persona ajena a la lista; el backend ya no
-- rechaza esos casos, solo los deja marcados aquí para auditoría.
--
-- Columna aditiva nullable: las parcialidades históricas quedan en NULL. Sin
-- backfill.
--
-- Hibernate (ddl-auto=update) también crea esta columna al iniciar el backend;
-- este script queda disponible para despliegues donde se prefiera migrar antes
-- de levantar la nueva versión.
--
-- ANTES DE EJECUTAR: respaldo completo de la base de datos.

ALTER TABLE operation_return_installments
  ADD COLUMN recibio_persona_autorizada BIT NULL
  AFTER persona_que_recibio_efectivo;

-- ============================================================================
-- Verificación:
--   SELECT id, estatus, persona_que_recibio_efectivo, recibio_persona_autorizada
--     FROM operation_return_installments
--    WHERE tipo_pago IN ('EFECTIVO', 'RETIRO_SIN_TARJETA');
-- ============================================================================
