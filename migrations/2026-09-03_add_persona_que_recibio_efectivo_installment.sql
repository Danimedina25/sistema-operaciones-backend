-- Persona autorizada que recibió físicamente el efectivo (o realizó el retiro
-- sin tarjeta) al cerrar una parcialidad de retorno. Se captura en la misma
-- transición ENTREGADA → COMPLETADA que la fotografía de entrega y debe
-- coincidir con uno de los autorizados registrados en la solicitud
-- (operation_return_payments.autorizado_para_recibir_efectivo_1..3).
--
-- Es distinta de entregado_por (usuario interno del sistema que cerró la
-- entrega): esa columna no cambia.
--
-- NULLABLE a propósito: las parcialidades cerradas antes de esta migración no
-- tienen el dato y se muestran como "No registrado (entrega histórica)". El
-- servicio la exige para todos los cierres nuevos de efectivo / retiro sin
-- tarjeta.
--
-- Hibernate (ddl-auto=update) también crea esta columna al iniciar el backend;
-- este script queda disponible para despliegues donde se prefiera migrar antes
-- de levantar la nueva versión.
--
-- ANTES DE EJECUTAR: respaldo completo de la base de datos.

ALTER TABLE operation_return_installments
  ADD COLUMN persona_que_recibio_efectivo VARCHAR(200) NULL
  AFTER comprobante_entrega_url;

-- ============================================================================
-- Verificación:
--   SHOW CREATE TABLE operation_return_installments;
--   SELECT id, estatus, tipo_pago, persona_que_recibio_efectivo
--     FROM operation_return_installments
--    WHERE tipo_pago IN ('EFECTIVO', 'RETIRO_SIN_TARJETA');
-- ============================================================================
