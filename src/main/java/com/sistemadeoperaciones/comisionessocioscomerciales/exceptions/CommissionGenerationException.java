package com.sistemadeoperaciones.comisionessocioscomerciales.exceptions;

import com.sistemadeoperaciones.shared.exception.BusinessException;

public class CommissionGenerationException extends BusinessException {

    public CommissionGenerationException() {
        super("No fue posible generar las comisiones");
    }

    public CommissionGenerationException(String message) {
        super(message);
    }
}