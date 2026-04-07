package com.sistemadeoperaciones.pagos.exceptions;

import com.sistemadeoperaciones.shared.exception.BusinessException;

public class MissingRejectionReasonException extends BusinessException {

    public MissingRejectionReasonException() {
        super("El motivo de rechazo es obligatorio");
    }
}