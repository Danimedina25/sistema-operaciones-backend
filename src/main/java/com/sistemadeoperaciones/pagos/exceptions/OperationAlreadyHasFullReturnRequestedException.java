package com.sistemadeoperaciones.pagos.exceptions;

import com.sistemadeoperaciones.shared.exception.BusinessException;

public class OperationAlreadyHasFullReturnRequestedException extends BusinessException {

    public OperationAlreadyHasFullReturnRequestedException() {
        super("La operación ya tiene solicitado el retorno total.");
    }
}