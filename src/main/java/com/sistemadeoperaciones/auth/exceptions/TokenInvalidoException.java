package com.sistemadeoperaciones.auth.exceptions;

import com.sistemadeoperaciones.shared.exception.BusinessException;

public class TokenInvalidoException extends BusinessException {

    public TokenInvalidoException() {
        super("Token inválido");
    }

    public TokenInvalidoException(String message) {
        super(message);
    }
}