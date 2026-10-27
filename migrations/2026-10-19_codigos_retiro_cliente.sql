-- NULL conserva el flujo histórico de Cajas y su contabilidad.
ALTER TABLE operation_return_installments
 ADD COLUMN codigo_para_cliente BIT NULL,
 ADD COLUMN fecha_cobro_bancario DATETIME NULL;
