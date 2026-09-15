package com.sistemadeoperaciones.pagos.dto.retornos;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.sistemadeoperaciones.cajageneral.dto.CashQuantityDeserializer;
import com.sistemadeoperaciones.cajageneral.enums.CashDenomination;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.Map;

/**
 * Cuerpo del endpoint legacy {@code PATCH /payments/{id}/mark-cash-delivered}.
 * Delega en el cierre por parcialidad, por lo que ahora también exige la persona
 * autorizada que recibió realmente el efectivo / realizó el retiro sin tarjeta.
 * El frontend nuevo ya no usa este endpoint (cierra por parcialidad con
 * {@code PATCH /installments/{id}/deliver}).
 */
public class MarkCashReturnDeliveredRequestDto {
    @NotBlank(message = "El comprobante de entrega de efectivo es obligatorio")
    @Size(max = 500, message = "La URL del comprobante no puede exceder 500 caracteres")
    private String comprobanteEntregaEfectivoUrl;

    @NotBlank(message = "La persona que recibió el efectivo es obligatoria")
    @Size(max = 200, message = "El nombre de la persona que recibió no puede exceder 200 caracteres")
    private String personaQueRecibioEfectivo;

    @JsonDeserialize(contentUsing = CashQuantityDeserializer.class)
    private Map<CashDenomination, @NotNull @Min(0) Integer> denominaciones;

    public String getComprobanteEntregaEfectivoUrl() {
        return comprobanteEntregaEfectivoUrl;
    }

    public void setComprobanteEntregaEfectivoUrl(String comprobanteEntregaEfectivoUrl) {
        this.comprobanteEntregaEfectivoUrl = comprobanteEntregaEfectivoUrl;
    }

    public String getPersonaQueRecibioEfectivo() {
        return personaQueRecibioEfectivo;
    }

    public void setPersonaQueRecibioEfectivo(String personaQueRecibioEfectivo) {
        this.personaQueRecibioEfectivo = personaQueRecibioEfectivo;
    }

    public Map<CashDenomination, Integer> getDenominaciones() {
        return denominaciones;
    }

    public void setDenominaciones(Map<CashDenomination, Integer> denominaciones) {
        this.denominaciones = denominaciones;
    }
}
