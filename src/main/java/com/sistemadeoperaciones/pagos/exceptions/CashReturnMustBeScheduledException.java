package com.sistemadeoperaciones.pagos.exceptions;

import com.sistemadeoperaciones.shared.exception.BusinessException;

public class CashReturnMustBeScheduledException extends BusinessException {

    public CashReturnMustBeScheduledException() {
        super("Los retornos en efectivo no se realizan desde este proceso. Primero debe registrarse la fecha y hora de recolección.");
    }
}