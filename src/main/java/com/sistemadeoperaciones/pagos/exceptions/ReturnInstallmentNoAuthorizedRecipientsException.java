package com.sistemadeoperaciones.pagos.exceptions;

import com.sistemadeoperaciones.shared.exception.BusinessException;

/**
 * La solicitud de retorno no tiene ninguna persona autorizada para recibir, por
 * lo que no se puede cerrar la entrega en efectivo / retiro sin tarjeta. Hay que
 * actualizar la solicitud antes. HTTP 400.
 */
public class ReturnInstallmentNoAuthorizedRecipientsException extends BusinessException {

    public ReturnInstallmentNoAuthorizedRecipientsException() {
        super("Esta solicitud no tiene personas autorizadas para recibir.");
    }
}
