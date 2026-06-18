package com.sistemadeoperaciones.corte.exceptions;

import com.sistemadeoperaciones.shared.exception.BusinessException;

public class InvalidCashCutDateRangeException extends BusinessException {

    public InvalidCashCutDateRangeException() {
        super("El rango de fechas del corte no es válido");
    }

    public InvalidCashCutDateRangeException(String message) {
        super(message);
    }
}