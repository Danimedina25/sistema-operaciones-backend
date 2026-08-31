package com.sistemadeoperaciones.pagos.exceptions;

import com.sistemadeoperaciones.shared.exception.BusinessException;

public class ReturnInstallmentAmountExceedsAvailableException extends BusinessException {

    public ReturnInstallmentAmountExceedsAvailableException() {
        super("El monto de la parcialidad excede el saldo disponible de la solicitud");
    }
}
