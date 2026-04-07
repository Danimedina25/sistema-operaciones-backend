package com.sistemadeoperaciones.auth.exceptions;

import com.sistemadeoperaciones.shared.exception.BusinessException;

public class UsuarioInactivoException extends BusinessException {

    public UsuarioInactivoException() {
        super("El usuario se encuentra inactivo");
    }

    public UsuarioInactivoException(String message) {
        super(message);
    }
}