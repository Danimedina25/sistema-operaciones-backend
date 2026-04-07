package com.sistemadeoperaciones.pagos.exceptions;

import com.sistemadeoperaciones.shared.exception.ResourceNotFoundException;

public class OperationPaymentNotFoundException extends ResourceNotFoundException {

    public OperationPaymentNotFoundException(Long id) {
        super("Pago no encontrado con id: " + id);
    }
}