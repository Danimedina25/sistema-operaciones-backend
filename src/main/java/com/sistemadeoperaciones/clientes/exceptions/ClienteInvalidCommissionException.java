package com.sistemadeoperaciones.clientes.exceptions;

import com.sistemadeoperaciones.shared.exception.BusinessException;

public class ClienteInvalidCommissionException extends BusinessException {

    public ClienteInvalidCommissionException() {
        super("El porcentaje de comisión no es válido");
    }

    public ClienteInvalidCommissionException(String message) {
        super(message);
    }
}