package com.sistemadeoperaciones.pagos.exceptions;

import com.sistemadeoperaciones.shared.exception.BusinessException;

public class ReturnInstallmentInvalidStatusException extends BusinessException {

    public ReturnInstallmentInvalidStatusException(String detalle) {
        super(detalle);
    }

    public ReturnInstallmentInvalidStatusException() {
        super("La parcialidad no está en un estatus que permita esta acción");
    }
}
