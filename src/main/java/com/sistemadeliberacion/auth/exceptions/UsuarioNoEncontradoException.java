package com.sistemadeliberacion.auth.exceptions;

import com.sistemadeliberacion.shared.exception.BusinessException;

public class UsuarioNoEncontradoException extends BusinessException {

    public UsuarioNoEncontradoException() {
        super("Usuario no encontrado");
    }

    public UsuarioNoEncontradoException(String message) {
        super(message);
    }
}