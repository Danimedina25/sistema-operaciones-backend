package com.sistemadeoperaciones.usuarios.exceptions;

import com.sistemadeoperaciones.shared.exception.BusinessException;

public class CuentaBancariaRequiredException extends BusinessException {

    public CuentaBancariaRequiredException() {
        super("La cuenta bancaria es obligatoria para usuarios con rol SOCIO_COMERCIAL");
    }

    public CuentaBancariaRequiredException(String message) {
        super(message);
    }
}