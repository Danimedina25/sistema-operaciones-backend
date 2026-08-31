package com.sistemadeoperaciones.pagos.dto.retornos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class DeliverReturnInstallmentRequestDto {

    @NotBlank(message = "El comprobante de entrega es obligatorio")
    @Size(max = 500, message = "La URL del comprobante no puede exceder 500 caracteres")
    private String comprobanteEntregaUrl;

    public String getComprobanteEntregaUrl() {
        return comprobanteEntregaUrl;
    }

    public void setComprobanteEntregaUrl(String comprobanteEntregaUrl) {
        this.comprobanteEntregaUrl = comprobanteEntregaUrl;
    }
}
