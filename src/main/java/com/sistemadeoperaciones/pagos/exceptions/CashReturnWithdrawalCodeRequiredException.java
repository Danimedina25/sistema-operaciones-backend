package com.sistemadeoperaciones.pagos.exceptions;

import com.sistemadeoperaciones.shared.exception.BusinessException;

public class CashReturnWithdrawalCodeRequiredException extends BusinessException {

    public CashReturnWithdrawalCodeRequiredException() {
        super("El código de retiro sin tarjeta es obligatorio");
    }
}
