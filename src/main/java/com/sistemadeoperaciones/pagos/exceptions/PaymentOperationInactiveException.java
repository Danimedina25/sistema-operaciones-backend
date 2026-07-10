package com.sistemadeoperaciones.pagos.exceptions;

import com.sistemadeoperaciones.shared.exception.BusinessException;

public class PaymentOperationInactiveException extends BusinessException {

    public PaymentOperationInactiveException() {
        super("Esta operación está desactivada y no se puede visualizar su detalle");
    }
}
