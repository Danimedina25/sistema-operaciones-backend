package com.sistemadeoperaciones.corte.exceptions;

import com.sistemadeoperaciones.shared.exception.BusinessException;

import java.time.LocalDate;

public class DailyCashCutAlreadyExistsException extends BusinessException {

    public DailyCashCutAlreadyExistsException(LocalDate fecha) {
        super("Ya existe un corte registrado para la fecha " + fecha);
    }

    public DailyCashCutAlreadyExistsException(String message) {
        super(message);
    }
}