package com.sistemadeoperaciones.corte.exceptions;

import com.sistemadeoperaciones.shared.exception.BusinessException;

public class FutureCashCutDateException extends BusinessException {

    public FutureCashCutDateException() {
        super("No se puede consultar el corte de una fecha futura");
    }
}
