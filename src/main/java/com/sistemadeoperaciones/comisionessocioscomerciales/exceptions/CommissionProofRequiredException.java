package com.sistemadeoperaciones.comisionessocioscomerciales.exceptions;

import com.sistemadeoperaciones.shared.exception.BusinessException;

public class CommissionProofRequiredException extends BusinessException {

    public CommissionProofRequiredException() {
        super("El comprobante de pago es obligatorio");
    }

    public CommissionProofRequiredException(String message) {
        super(message);
    }
}