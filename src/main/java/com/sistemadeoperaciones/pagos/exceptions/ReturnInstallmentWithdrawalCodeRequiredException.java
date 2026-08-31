package com.sistemadeoperaciones.pagos.exceptions;

import com.sistemadeoperaciones.shared.exception.BusinessException;

public class ReturnInstallmentWithdrawalCodeRequiredException extends BusinessException {

    public ReturnInstallmentWithdrawalCodeRequiredException() {
        super("El código de retiro sin tarjeta es obligatorio para esta parcialidad");
    }
}
