package com.sistemadeoperaciones.pagos.exceptions;

import com.sistemadeoperaciones.shared.exception.ResourceNotFoundException;

public class PaymentOperationNotFoundException extends ResourceNotFoundException {

    public PaymentOperationNotFoundException(Long id) {
        super("Operación no encontrada con id: " + id);
    }
}