package com.sistemadeoperaciones.pagos.exceptions;

import com.sistemadeoperaciones.shared.exception.BusinessException;

public class ReturnInstallmentPickupDateRequiredException extends BusinessException {

    public ReturnInstallmentPickupDateRequiredException() {
        super("La fecha y hora de recolección es obligatoria para esta parcialidad");
    }
}
