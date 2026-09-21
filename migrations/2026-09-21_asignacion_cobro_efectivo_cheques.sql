-- Permite transferir la responsabilidad de un cheque desde Cuentas hacia Caja
-- sin reconocer el ingreso hasta que Caja confirme la recepción física del efectivo.
ALTER TABLE operation_payments
    DROP CHECK chk_cheque_estado,
    ADD CONSTRAINT chk_cheque_estado CHECK (
        cheque_estado IS NULL OR
        (tipo_pago = 'CHEQUE' AND cheque_estado IN (
            'POR_COBRAR',
            'DEPOSITADO',
            'PENDIENTE_COBRO_EFECTIVO',
            'COBRADO',
            'DEVUELTO',
            'CANCELADO'
        ))
    );

-- Producción conserva NotificationType como ENUM; Hibernate no añade valores
-- nuevos a un ENUM existente mediante ddl-auto=update.
ALTER TABLE notifications
    MODIFY COLUMN tipo ENUM(
        'OPERATION_CREATED',
        'PAYMENT_SUBMITTED',
        'PAYMENT_TYPE_CHANGED',
        'PAYMENT_VALIDATED',
        'PAYMENT_REJECTED',
        'OPERATION_STATUS_CHANGED',
        'COMMISSION_PAID',
        'CASH_RETURN_REQUESTED',
        'CHEQUE_CASH_COLLECTION_ASSIGNED',
        'CHEQUE_CASH_COLLECTION_RETURNED',
        'CHEQUE_CASH_COLLECTION_WITHDRAWN',
        'CHEQUE_CASH_COLLECTION_COMPLETED',
        'RETURN_INSTALLMENT_SCHEDULED',
        'RETURN_INSTALLMENT_CODE_AVAILABLE',
        'RETURN_INSTALLMENT_DELIVERED',
        'RETURN_INSTALLMENT_COMPLETED',
        'RETURN_INSTALLMENT_CANCELLED',
        'RETURN_REQUEST_COMPLETED',
        'SYSTEM_ALERT'
    ) NOT NULL;
