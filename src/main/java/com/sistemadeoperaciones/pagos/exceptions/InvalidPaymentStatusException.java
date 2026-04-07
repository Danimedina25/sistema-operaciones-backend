package com.sistemadeoperaciones.pagos.exceptions;

import com.sistemadeoperaciones.shared.exception.BusinessException;

public class InvalidPaymentStatusException extends BusinessException {

    public InvalidPaymentStatusException(String message) {
        super(message);
    }
}