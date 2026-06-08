package com.sistemadeoperaciones.pagos.exceptions;

import com.sistemadeoperaciones.shared.exception.BusinessException;

public class ReturnAmountExceedsPendingBalanceException extends BusinessException {

    public ReturnAmountExceedsPendingBalanceException() {
        super("El monto solicitado para retorno excede el saldo pendiente por devolver");
    }
}