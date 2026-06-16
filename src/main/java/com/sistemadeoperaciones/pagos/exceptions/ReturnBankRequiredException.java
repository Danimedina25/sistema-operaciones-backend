package com.sistemadeoperaciones.pagos.exceptions;

import com.sistemadeoperaciones.shared.exception.BusinessException;

public class ReturnBankRequiredException
        extends BusinessException {

    public ReturnBankRequiredException() {
        super("El banco destino es obligatorio");
    }
}