package com.sistemadeoperaciones.pagos.dto.retornos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class CancelReturnInstallmentRequestDto {

    @NotBlank(message = "El motivo de cancelación es obligatorio")
    @Size(max = 500, message = "El motivo no puede superar los 500 caracteres")
    private String motivo;

    public String getMotivo() {
        return motivo;
    }

    public void setMotivo(String motivo) {
        this.motivo = motivo;
    }
}
