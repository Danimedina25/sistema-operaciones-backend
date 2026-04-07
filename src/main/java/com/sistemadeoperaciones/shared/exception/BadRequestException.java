package com.sistemadeoperaciones.shared.exception;

public class BadRequestException extends BusinessException {

    public BadRequestException() {
        super("Solicitud inválida");
    }

    public BadRequestException(String message) {
        super(message);
    }
}