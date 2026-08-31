package com.sistemadeoperaciones.pagos.exceptions;

import com.sistemadeoperaciones.shared.exception.BusinessException;

public class ReturnInstallmentReceiptRequiredException extends BusinessException {

    public ReturnInstallmentReceiptRequiredException() {
        super("El comprobante de la parcialidad es obligatorio");
    }
}
