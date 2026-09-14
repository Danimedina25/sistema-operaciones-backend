package com.sistemadeoperaciones.cajageneral.exceptions;
import com.sistemadeoperaciones.shared.exception.BadRequestException;
public class InvalidCashGeneralException extends BadRequestException {
    public InvalidCashGeneralException(String message) { super(message); }
}
