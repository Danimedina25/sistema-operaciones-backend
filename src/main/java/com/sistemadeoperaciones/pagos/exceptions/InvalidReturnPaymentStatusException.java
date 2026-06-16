package com.sistemadeoperaciones.pagos.exceptions;

import com.sistemadeoperaciones.shared.exception.BusinessException;

public class InvalidReturnPaymentStatusException extends BusinessException {

    public InvalidReturnPaymentStatusException() {
        super("Solo se pueden realizar retornos solicitados");
    }
}