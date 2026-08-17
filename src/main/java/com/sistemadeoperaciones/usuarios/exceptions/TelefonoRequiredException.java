package com.sistemadeoperaciones.usuarios.exceptions;

import com.sistemadeoperaciones.shared.exception.BusinessException;

public class TelefonoRequiredException extends BusinessException {

    public TelefonoRequiredException() {
        super("El teléfono es obligatorio para usuarios con rol SOCIO_COMERCIAL, JEFA_CAJAS, JEFA_CUENTAS o AUXILIAR_CUENTAS");
    }

    public TelefonoRequiredException(String message) {
        super(message);
    }
}
