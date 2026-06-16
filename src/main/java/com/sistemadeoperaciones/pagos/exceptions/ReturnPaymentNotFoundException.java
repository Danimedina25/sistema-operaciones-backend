package com.sistemadeoperaciones.pagos.exceptions;

import com.sistemadeoperaciones.shared.exception.BusinessException;

public class ReturnPaymentNotFoundException
        extends BusinessException {

    public ReturnPaymentNotFoundException() {
        super("La solicitud de retorno no existe");
    }
}