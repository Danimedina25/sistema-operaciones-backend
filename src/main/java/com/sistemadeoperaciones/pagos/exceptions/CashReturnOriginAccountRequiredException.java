package com.sistemadeoperaciones.pagos.exceptions;

import com.sistemadeoperaciones.shared.exception.BusinessException;

public class CashReturnOriginAccountRequiredException extends BusinessException {

    public CashReturnOriginAccountRequiredException() {
        super("La cuenta origen es obligatoria para retiros sin tarjeta");
    }
}
