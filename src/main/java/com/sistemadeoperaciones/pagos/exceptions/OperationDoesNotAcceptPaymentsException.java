package com.sistemadeoperaciones.pagos.exceptions;

import com.sistemadeoperaciones.shared.exception.BusinessException;

public class OperationDoesNotAcceptPaymentsException extends BusinessException {

    public OperationDoesNotAcceptPaymentsException(String message) {
        super(message);
    }
}