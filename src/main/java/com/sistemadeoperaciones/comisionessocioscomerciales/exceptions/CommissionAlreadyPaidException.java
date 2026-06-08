package com.sistemadeoperaciones.comisionessocioscomerciales.exceptions;

import com.sistemadeoperaciones.shared.exception.BusinessException;

public class CommissionAlreadyPaidException extends BusinessException {

    public CommissionAlreadyPaidException() {
        super("La comisión ya fue pagada");
    }

    public CommissionAlreadyPaidException(String message) {
        super(message);
    }
}