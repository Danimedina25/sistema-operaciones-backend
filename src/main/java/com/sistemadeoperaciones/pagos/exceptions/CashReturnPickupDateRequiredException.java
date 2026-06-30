package com.sistemadeoperaciones.pagos.exceptions;

import com.sistemadeoperaciones.shared.exception.BusinessException;

public class CashReturnPickupDateRequiredException extends BusinessException {

    public CashReturnPickupDateRequiredException() {
        super("La fecha y hora de recolección del efectivo es obligatoria");
    }
}