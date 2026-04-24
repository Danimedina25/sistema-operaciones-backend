package com.sistemadeoperaciones.clientes.exceptions;

import com.sistemadeoperaciones.shared.exception.BusinessException;

public class ClienteNameRequiredException extends BusinessException {

    public ClienteNameRequiredException() {
        super("El nombre del cliente es obligatorio");
    }
}