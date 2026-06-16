package com.sistemadeoperaciones.pagos.exceptions;

import com.sistemadeoperaciones.shared.exception.BusinessException;

public class ReturnAccountHolderRequiredException
        extends BusinessException {

    public ReturnAccountHolderRequiredException() {
        super("El titular de la cuenta es obligatorio");
    }
}