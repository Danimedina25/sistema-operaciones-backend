package com.sistemadeoperaciones.pagos.exceptions;

import com.sistemadeoperaciones.shared.exception.BusinessException;

public class InvalidCashReturnPaymentTypeException extends BusinessException {

    public InvalidCashReturnPaymentTypeException() {
        super("Solo se puede definir la fecha y hora de recolección para retornos en efectivo");
    }
}