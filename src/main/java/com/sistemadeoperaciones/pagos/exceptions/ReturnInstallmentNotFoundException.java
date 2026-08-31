package com.sistemadeoperaciones.pagos.exceptions;

import com.sistemadeoperaciones.shared.exception.BusinessException;

public class ReturnInstallmentNotFoundException extends BusinessException {

    public ReturnInstallmentNotFoundException() {
        super("La parcialidad de retorno no existe");
    }
}
