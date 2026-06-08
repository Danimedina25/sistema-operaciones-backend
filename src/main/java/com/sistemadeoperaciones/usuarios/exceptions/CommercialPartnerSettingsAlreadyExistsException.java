package com.sistemadeoperaciones.usuarios.exceptions;

import com.sistemadeoperaciones.shared.exception.BusinessException;

public class CommercialPartnerSettingsAlreadyExistsException extends BusinessException {

    public CommercialPartnerSettingsAlreadyExistsException() {
        super(
            "El usuario ya cuenta con configuración comercial registrada"
        );
    }
}