package com.sistemadeliberacion.auth.exceptions;

import com.sistemadeliberacion.shared.exception.BusinessException;

public class TokenExpiradoException extends BusinessException {

    public TokenExpiradoException() {
        super("Token expirado");
    }

    public TokenExpiradoException(String message) {
        super(message);
    }
}