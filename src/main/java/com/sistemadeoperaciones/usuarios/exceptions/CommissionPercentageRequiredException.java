package com.sistemadeoperaciones.usuarios.exceptions;

import com.sistemadeoperaciones.shared.exception.BusinessException;

public class CommissionPercentageRequiredException extends BusinessException {

    public CommissionPercentageRequiredException() {
        super(
            "El porcentaje de comisión es obligatorio para usuarios con rol SOCIO_COMERCIAL"
        );
    }
}