package com.sistemadeoperaciones.pagos.exceptions;

import com.sistemadeoperaciones.shared.exception.BusinessException;

public class PaymentOperationInactivePartnerException extends BusinessException {

    public PaymentOperationInactivePartnerException() {
        super("El socio comercial se encuentra inactivo");
    }
}