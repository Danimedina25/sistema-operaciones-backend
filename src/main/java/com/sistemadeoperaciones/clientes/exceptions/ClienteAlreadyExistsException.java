package com.sistemadeoperaciones.clientes.exceptions;

import com.sistemadeoperaciones.shared.exception.BusinessException;

public class ClienteAlreadyExistsException extends BusinessException {

    public ClienteAlreadyExistsException() {
        super("Ya existe un cliente con ese nombre");
    }

    public ClienteAlreadyExistsException(String nombre) {
        super("Ya existe un cliente con el nombre: " + nombre);
    }
}