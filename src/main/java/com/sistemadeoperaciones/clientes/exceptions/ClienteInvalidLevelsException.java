package com.sistemadeoperaciones.clientes.exceptions;

import com.sistemadeoperaciones.shared.exception.BusinessException;

public class ClienteInvalidLevelsException extends BusinessException {

    public ClienteInvalidLevelsException() {
        super("El número de niveles de red comercial no es válido");
    }
}