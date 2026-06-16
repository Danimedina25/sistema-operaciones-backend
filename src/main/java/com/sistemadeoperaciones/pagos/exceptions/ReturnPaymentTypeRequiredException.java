package com.sistemadeoperaciones.pagos.exceptions;

import com.sistemadeoperaciones.shared.exception.BusinessException;

public class ReturnPaymentTypeRequiredException
        extends BusinessException {

    public ReturnPaymentTypeRequiredException() {
        super("El tipo de pago es obligatorio");
    }
}