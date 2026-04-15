package com.sistemadeoperaciones.usuarios.exceptions;
import com.sistemadeoperaciones.shared.exception.BusinessException;

public class EmailSendException extends BusinessException {

    public EmailSendException() {
        super("El email no pudo ser enviado");
    }

    public EmailSendException(String message) {
        super(message);
    }
}