package com.sistemadeoperaciones.pagos.exceptions;

import com.sistemadeoperaciones.shared.exception.BusinessException;

public class InvalidReturnAmountException extends BusinessException {

    public InvalidReturnAmountException() {
        super("El monto del retorno debe ser mayor a 0");
    }
}