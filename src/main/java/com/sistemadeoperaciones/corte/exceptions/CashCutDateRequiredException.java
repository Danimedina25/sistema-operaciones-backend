package com.sistemadeoperaciones.corte.exceptions;

import com.sistemadeoperaciones.shared.exception.BusinessException;

public class CashCutDateRequiredException extends BusinessException {

    public CashCutDateRequiredException() {
        super("La fecha del corte es obligatoria");
    }

    public CashCutDateRequiredException(String message) {
        super(message);
    }
}