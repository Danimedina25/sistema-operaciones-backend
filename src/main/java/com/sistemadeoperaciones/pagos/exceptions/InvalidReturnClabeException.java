package com.sistemadeoperaciones.pagos.exceptions;

import com.sistemadeoperaciones.shared.exception.BusinessException;

public class InvalidReturnClabeException
        extends BusinessException {

    public InvalidReturnClabeException() {
        super("La CLABE solo debe contener números");
    }
}