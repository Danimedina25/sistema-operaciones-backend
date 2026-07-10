package com.sistemadeoperaciones.pagos.exceptions;

import com.sistemadeoperaciones.shared.exception.BusinessException;

public class OperationCannotReceiveReturnException extends BusinessException {

    public OperationCannotReceiveReturnException() {
        super("La operación no se encuentra en un estatus que permita solicitar retornos.");
    }
}