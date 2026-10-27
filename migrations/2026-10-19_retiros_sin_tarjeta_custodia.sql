-- NULL en las nuevas marcas conserva los movimientos históricos.
ALTER TABLE operation_return_payments
    ADD COLUMN cuenta_asignada_en DATETIME NULL,
    ADD COLUMN cuenta_asignada_por BIGINT NULL;
ALTER TABLE operation_return_installments
    ADD COLUMN fecha_preparacion_bancaria DATETIME NULL;
