package com.sistemadeoperaciones.pagos.exceptions;

import com.sistemadeoperaciones.shared.exception.BusinessException;

public class PaymentOperationInactiveAccountException extends BusinessException {

    public PaymentOperationInactiveAccountException() {
        super("La cuenta destino se encuentra inactiva");
    }
}