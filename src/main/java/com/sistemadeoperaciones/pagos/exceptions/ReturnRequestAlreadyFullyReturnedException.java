package com.sistemadeoperaciones.pagos.exceptions;

import com.sistemadeoperaciones.shared.exception.BusinessException;

public class ReturnRequestAlreadyFullyReturnedException extends BusinessException {

    public ReturnRequestAlreadyFullyReturnedException() {
        super("La solicitud de retorno ya fue retornada por completo y no admite nuevas parcialidades");
    }
}
