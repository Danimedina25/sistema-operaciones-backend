package com.sistemadeoperaciones.pagos.exceptions;

import com.sistemadeoperaciones.shared.exception.BusinessException;

public class PaymentAmountExceededException extends BusinessException {

    public PaymentAmountExceededException() {
        super("La suma de los pagos excede el monto total esperado de la operación");
    }
}