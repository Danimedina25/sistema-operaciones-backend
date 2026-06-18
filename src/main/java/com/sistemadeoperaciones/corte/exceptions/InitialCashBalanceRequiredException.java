package com.sistemadeoperaciones.corte.exceptions;

import com.sistemadeoperaciones.shared.exception.BusinessException;

public class InitialCashBalanceRequiredException extends BusinessException {

    public InitialCashBalanceRequiredException() {
        super("No existe un corte anterior. Es necesario indicar un saldo inicial manual para generar el primer corte");
    }

    public InitialCashBalanceRequiredException(String message) {
        super(message);
    }
}