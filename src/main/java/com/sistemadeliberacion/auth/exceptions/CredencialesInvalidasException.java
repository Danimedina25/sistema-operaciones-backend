package com.sistemadeliberacion.auth.exceptions;

import com.sistemadeliberacion.shared.exception.BusinessException;

public class CredencialesInvalidasException extends BusinessException {

    public CredencialesInvalidasException() {
        super("Credenciales inválidas");
    }

    public CredencialesInvalidasException(String message) {
        super(message);
    }
}