package com.sistemadeoperaciones.auth.exceptions;

import com.sistemadeoperaciones.shared.exception.BusinessException;

public class TokenExpiradoException extends BusinessException {

    public TokenExpiradoException() {
        super("Token expirado");
    }

    public TokenExpiradoException(String message) {
        super(message);
    }
}