package com.sistemadeliberacion.auth.exceptions;

import com.sistemadeliberacion.shared.exception.BusinessException;

public class TokenInvalidoException extends BusinessException {

    public TokenInvalidoException() {
        super("Token inválido");
    }

    public TokenInvalidoException(String message) {
        super(message);
    }
}