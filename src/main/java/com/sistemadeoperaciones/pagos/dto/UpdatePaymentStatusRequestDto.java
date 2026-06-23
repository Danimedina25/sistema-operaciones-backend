package com.sistemadeoperaciones.pagos.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class UpdatePaymentStatusRequestDto {

    @Size(max = 500, message = "Las observaciones no pueden exceder 500 caracteres")
    private String observaciones;
    @Size(max = 500, message = "La URL del comprobante de validación no puede exceder 500 caracteres")
    private String comprobanteValidacionUrl;

    public String getObservaciones() {
        return observaciones;
    }

    public void setObservaciones(String observaciones) {
        this.observaciones = observaciones;
    }

    public String getComprobanteValidacionUrl() {
        return comprobanteValidacionUrl;
    }

    public void setComprobanteValidacionUrl(String comprobanteValidacionUrl) {
        this.comprobanteValidacionUrl = comprobanteValidacionUrl;
    }
}