package com.sistemadeoperaciones.pagos.exceptions;

import com.sistemadeoperaciones.shared.exception.BusinessException;

public class ReturnInstallmentNotCancellableException extends BusinessException {

    public ReturnInstallmentNotCancellableException() {
        super("Solo pueden cancelarse parcialidades que aún no se han completado");
    }
}
