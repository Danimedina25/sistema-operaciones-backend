package com.sistemadeoperaciones.pagos.exceptions;

import com.sistemadeoperaciones.shared.exception.BusinessException;

public class ReturnCashPaymentCannotContainBankDataException
        extends BusinessException {

    public ReturnCashPaymentCannotContainBankDataException() {
        super(
            "Para retornos en efectivo no se deben enviar datos bancarios"
        );
    }
}