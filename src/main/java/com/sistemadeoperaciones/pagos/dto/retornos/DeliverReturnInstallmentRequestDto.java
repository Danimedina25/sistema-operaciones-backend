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
 * Cierre final de una parcialidad de retorno en efectivo / retiro sin tarjeta
 * (ENTREGADA → COMPLETADA). Ambos datos se guardan en la misma transición: no
 * se puede cerrar sin la fotografía de entrega y sin la persona autorizada que
 * recibió realmente los fondos.
 */
public class DeliverReturnInstallmentRequestDto {

    @NotBlank(message = "El comprobante de entrega es obligatorio")
    @Size(max = 500, message = "La URL del comprobante no puede exceder 500 caracteres")
    private String comprobanteEntregaUrl;

    /**
     * Nombre de la persona autorizada que recibió el efectivo (o realizó el
     * retiro sin tarjeta). El servicio lo normaliza y valida contra los
     * autorizados de la solicitud; guarda el nombre canónico registrado ahí.
     */
    @NotBlank(message = "La persona que recibió el efectivo es obligatoria")
    @Size(max = 200, message = "El nombre de la persona que recibió no puede exceder 200 caracteres")
    private String personaQueRecibioEfectivo;

    /** Requerido por negocio únicamente para entregas de efectivo físico. */
    @JsonDeserialize(contentUsing = CashQuantityDeserializer.class)
    private Map<CashDenomination, @NotNull @Min(0) Integer> denominaciones;

    public String getComprobanteEntregaUrl() {
        return comprobanteEntregaUrl;
    }

    public void setComprobanteEntregaUrl(String comprobanteEntregaUrl) {
        this.comprobanteEntregaUrl = comprobanteEntregaUrl;
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
