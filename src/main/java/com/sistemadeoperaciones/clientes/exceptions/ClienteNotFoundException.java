package com.sistemadeoperaciones.clientes.exceptions;

import com.sistemadeoperaciones.shared.exception.ResourceNotFoundException;

public class ClienteNotFoundException extends ResourceNotFoundException {

    public ClienteNotFoundException(Long id) {
        super("Cliente no encontrado con id: " + id);
    }
}