package com.sistemadeoperaciones.auth.exceptions;

import com.sistemadeoperaciones.shared.exception.BusinessException;

public class CredencialesInvalidasException extends BusinessException {

    public CredencialesInvalidasException() {
        super("Credenciales inválidas");
    }

    public CredencialesInvalidasException(String message) {
        super(message);
    }
}