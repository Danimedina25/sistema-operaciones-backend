package com.sistemadeoperaciones.pagos.exceptions;

import com.sistemadeoperaciones.shared.exception.BusinessException;

public class ReturnInstallmentOriginAccountRequiredException extends BusinessException {

    public ReturnInstallmentOriginAccountRequiredException() {
        super("La cuenta origen es obligatoria para esta parcialidad");
    }
}
