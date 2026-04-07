package com.sistemadeoperaciones.pagos.dto;

import jakarta.validation.constraints.Size;

public class UpdatePaymentStatusRequestDto {

    @Size(max = 500, message = "Las observaciones no pueden exceder 500 caracteres")
    private String observaciones;

    public String getObservaciones() {
        return observaciones;
    }

    public void setObservaciones(String observaciones) {
        this.observaciones = observaciones;
    }
}