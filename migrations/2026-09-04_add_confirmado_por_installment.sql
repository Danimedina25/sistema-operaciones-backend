-- Usuario (socio comercial) que confirmó la recepción de una recolección de
-- efectivo / retiro sin tarjeta. Es la contraparte de entregado_por (jefa de
-- cajas que cerró la entrega con foto + persona que recibió).
--
-- Con este cambio las dos confirmaciones son INDEPENDIENTES: cada parte registra
-- la suya en cualquier orden y la parcialidad queda COMPLETADA solo cuando están
-- ambas (fecha_confirmacion != NULL Y fecha_entrega != NULL). El estatus
-- ENTREGADA pasa a significar "una de las dos marcas presente".
--
-- Columna aditiva nullable: las parcialidades históricas la tienen en NULL y no
-- se recalculan (confirm/deliver rechazan COMPLETADA y CANCELADA). Sin backfill.
--
-- Hibernate (ddl-auto=update) también crea esta columna al iniciar el backend;
-- este script queda disponible para despliegues donde se prefiera migrar antes
-- de levantar la nueva versión.
--
-- ANTES DE EJECUTAR: respaldo completo de la base de datos.

ALTER TABLE operation_return_installments
  ADD COLUMN confirmado_por BIGINT NULL
  AFTER entregado_por;

ALTER TABLE operation_return_installments
  ADD CONSTRAINT fk_return_installment_confirmado_por
  FOREIGN KEY (confirmado_por) REFERENCES users (id);

-- ============================================================================
-- Verificación:
--   SHOW CREATE TABLE operation_return_installments;
--   SELECT id, estatus, tipo_pago, fecha_confirmacion, confirmado_por,
--          fecha_entrega, entregado_por
--     FROM operation_return_installments
--    WHERE tipo_pago IN ('EFECTIVO', 'RETIRO_SIN_TARJETA');
-- ============================================================================
