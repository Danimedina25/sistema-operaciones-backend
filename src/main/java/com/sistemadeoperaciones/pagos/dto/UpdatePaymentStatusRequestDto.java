package com.sistemadeoperaciones.pagos.dto;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.sistemadeoperaciones.cajageneral.dto.CashQuantityDeserializer;
import com.sistemadeoperaciones.cajageneral.enums.CashDenomination;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.Map;

public class UpdatePaymentStatusRequestDto {

    @Size(max = 500, message = "Las observaciones no pueden exceder 500 caracteres")
    private String observaciones;
    @Size(max = 500, message = "La URL del comprobante de validación no puede exceder 500 caracteres")
    private String comprobanteValidacionUrl;

    /**
     * Desglose del efectivo recibido. Obligatorio sólo al validar un pago EFECTIVO: esa
     * validación mete el dinero a Caja General, y allí todo movimiento exige su desglose.
     */
    @JsonDeserialize(contentUsing = CashQuantityDeserializer.class)
    private Map<CashDenomination, @NotNull @Min(0) Integer> denominaciones;

    public Map<CashDenomination, Integer> getDenominaciones() {
        return denominaciones;
    }

    public void setDenominaciones(Map<CashDenomination, Integer> denominaciones) {
        this.denominaciones = denominaciones;
    }

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