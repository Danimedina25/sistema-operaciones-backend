package com.sistemadeoperaciones.usuarios.exceptions;

import com.sistemadeoperaciones.shared.exception.BusinessException;

public class TitularCuentaRequiredException extends BusinessException {

    public TitularCuentaRequiredException() {
        super("El titular de la cuenta es obligatorio para usuarios con rol SOCIO_COMERCIAL");
    }

    public TitularCuentaRequiredException(String message) {
        super(message);
    }
}