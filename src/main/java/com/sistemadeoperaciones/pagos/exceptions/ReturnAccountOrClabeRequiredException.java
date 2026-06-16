package com.sistemadeoperaciones.pagos.exceptions;

import com.sistemadeoperaciones.shared.exception.BusinessException;

public class ReturnAccountOrClabeRequiredException
        extends BusinessException {

    public ReturnAccountOrClabeRequiredException() {
        super("Captura al menos número de cuenta o CLABE");
    }
}