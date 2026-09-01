package com.sistemadeoperaciones.pagos.exceptions;

import com.sistemadeoperaciones.shared.exception.BusinessException;

/**
 * Falta la persona autorizada que recibió el efectivo al cerrar una parcialidad
 * de retorno en efectivo / retiro sin tarjeta. HTTP 400.
 */
public class ReturnInstallmentReceiverRequiredException extends BusinessException {

    public ReturnInstallmentReceiverRequiredException() {
        super("La persona que recibió el efectivo es obligatoria");
    }
}
