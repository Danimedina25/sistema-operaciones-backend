package com.sistemadeoperaciones.usuarios.exceptions;

import com.sistemadeoperaciones.shared.exception.BusinessException;

public class BancoRequiredException extends BusinessException {

    public BancoRequiredException() {
        super("El banco es obligatorio para usuarios con rol SOCIO_COMERCIAL");
    }

    public BancoRequiredException(String message) {
        super(message);
    }
}