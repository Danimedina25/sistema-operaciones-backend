-- Evidencia fotográfica al cerrar retornos en efectivo/retiro sin tarjeta.
-- Hibernate (ddl-auto=update) también crea esta columna al iniciar el backend;
-- este script queda disponible para despliegues donde se prefiera migrar antes.
ALTER TABLE operation_return_payments
  ADD COLUMN comprobante_entrega_efectivo_url VARCHAR(500) NULL
  AFTER comprobante_url;
