package com.sistemadeoperaciones.pagos.exceptions;

import com.sistemadeoperaciones.shared.exception.BusinessException;

public class DuplicatePaymentReceiptException extends BusinessException {

    public DuplicatePaymentReceiptException() {
        super("Ya existe un pago registrado con el mismo comprobante para esta operación");
    }
}