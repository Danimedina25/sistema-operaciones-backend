package com.sistemadeoperaciones.comisionessocioscomerciales.exceptions;

import com.sistemadeoperaciones.shared.exception.BusinessException;

public class InvalidCommissionStructureException extends BusinessException {

    public InvalidCommissionStructureException() {
        super(
            "La estructura de la red comercial es inválida para generar las comisiones"
        );
    }

    public InvalidCommissionStructureException(String message) {
        super(message);
    }
}