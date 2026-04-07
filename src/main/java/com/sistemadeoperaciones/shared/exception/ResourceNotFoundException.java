package com.sistemadeoperaciones.shared.exception;

public class ResourceNotFoundException extends BusinessException {

    public ResourceNotFoundException() {
        super("Recurso no encontrado");
    }

    public ResourceNotFoundException(String message) {
        super(message);
    }
}