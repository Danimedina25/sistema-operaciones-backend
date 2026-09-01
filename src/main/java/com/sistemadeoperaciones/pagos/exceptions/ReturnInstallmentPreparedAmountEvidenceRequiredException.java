package com.sistemadeoperaciones.pagos.exceptions;

import com.sistemadeoperaciones.shared.exception.BusinessException;

public class ReturnInstallmentPreparedAmountEvidenceRequiredException extends BusinessException {

    public ReturnInstallmentPreparedAmountEvidenceRequiredException() {
        super("La evidencia del importe preparado es obligatoria");
    }
}
