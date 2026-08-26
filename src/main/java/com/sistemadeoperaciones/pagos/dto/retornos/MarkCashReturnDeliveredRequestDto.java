package com.sistemadeoperaciones.pagos.dto.retornos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class MarkCashReturnDeliveredRequestDto {
    @NotBlank(message = "El comprobante de entrega de efectivo es obligatorio")
    @Size(max = 500, message = "La URL del comprobante no puede exceder 500 caracteres")
    private String comprobanteEntregaEfectivoUrl;

    public String getComprobanteEntregaEfectivoUrl() {
        return comprobanteEntregaEfectivoUrl;
    }

    public void setComprobanteEntregaEfectivoUrl(String comprobanteEntregaEfectivoUrl) {
        this.comprobanteEntregaEfectivoUrl = comprobanteEntregaEfectivoUrl;
    }
}
