package com.sistemadeoperaciones.comisionessocioscomerciales.exceptions;

import com.sistemadeoperaciones.shared.exception.BusinessException;

public class CommissionRegenerationNotAllowedException extends BusinessException {

    public CommissionRegenerationNotAllowedException() {
        super("No es posible regenerar una operación que ya contiene comisiones pagadas");
    }

    public CommissionRegenerationNotAllowedException(String message) {
        super(message);
    }
}