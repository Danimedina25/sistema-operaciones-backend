package com.sistemadeoperaciones.pagos.exceptions;

import com.sistemadeoperaciones.shared.exception.BusinessException;

/**
 * La persona enviada al cerrar la parcialidad no coincide (tras normalizar
 * espacios, mayúsculas y acentos) con ninguno de los autorizados registrados en
 * la solicitud de retorno. HTTP 400.
 */
public class ReturnInstallmentReceiverNotAuthorizedException extends BusinessException {

    public ReturnInstallmentReceiverNotAuthorizedException() {
        super("La persona seleccionada no está autorizada para recibir esta entrega.");
    }
}
