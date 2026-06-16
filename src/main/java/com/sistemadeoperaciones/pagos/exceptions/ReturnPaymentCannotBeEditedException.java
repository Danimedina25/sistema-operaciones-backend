package com.sistemadeoperaciones.pagos.exceptions;

import com.sistemadeoperaciones.shared.exception.BusinessException;

public class ReturnPaymentCannotBeEditedException
        extends BusinessException {

    public ReturnPaymentCannotBeEditedException() {
        super("Solo pueden editarse solicitudes con estatus SOLICITADO");
    }
}