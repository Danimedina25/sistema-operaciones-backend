package com.sistemadeoperaciones.pagos.exceptions;

import com.sistemadeoperaciones.shared.exception.BusinessException;

public class InvalidReturnClabeLengthException
        extends BusinessException {

    public InvalidReturnClabeLengthException() {
        super("La CLABE debe tener exactamente 18 dígitos");
    }
}