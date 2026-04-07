package com.sistemadeoperaciones.pagos.exceptions;

import com.sistemadeoperaciones.shared.exception.BusinessException;

public class MissingPaymentReceiptException extends BusinessException {

    public MissingPaymentReceiptException() {
        super("No se puede validar un pago sin comprobante");
    }
}