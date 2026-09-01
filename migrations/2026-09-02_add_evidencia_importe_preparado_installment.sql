-- Evidencia del importe ya contado y preparado para la recolección, en
-- parcialidades de retorno en efectivo / retiro sin tarjeta. Se captura al
-- programar la parcialidad y es obligatoria para esos métodos.
--
-- Hibernate (ddl-auto=update) también crea esta columna al iniciar el backend;
-- este script queda disponible para despliegues donde se prefiera migrar antes
-- de levantar la nueva versión.
--
-- ANTES DE EJECUTAR: respaldo completo de la base de datos.

ALTER TABLE operation_return_installments
  ADD COLUMN evidencia_importe_preparado_url VARCHAR(500) NULL
  AFTER comprobante_url;

-- ============================================================================
-- Verificación:
--   SHOW CREATE TABLE operation_return_installments;
-- ============================================================================
