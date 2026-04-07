package com.sistemadeoperaciones.auth.exceptions;

import com.sistemadeoperaciones.shared.exception.BusinessException;

public class UsuarioNoEncontradoException extends BusinessException {

    public UsuarioNoEncontradoException() {
        super("Usuario no encontrado");
    }

    public UsuarioNoEncontradoException(String message) {
        super(message);
    }
}