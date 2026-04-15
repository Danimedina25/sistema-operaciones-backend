package com.sistemadeoperaciones.pagos.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public class CreatePaymentOperationRequestDto {

    @NotBlank(message = "El nombre del cliente es obligatorio")
    @Size(max = 150, message = "El nombre del cliente no puede exceder 150 caracteres")
    private String clienteNombre;

    @NotNull(message = "El monto total es obligatorio")
    @DecimalMin(value = "0.01", message = "El monto total debe ser mayor a cero")
    private BigDecimal montoTotal;

    @NotNull(message = "El socio comercial es obligatorio")
    private Long socioComercialId;

    @NotNull(message = "La cantidad de niveles de red es obligatoria")
    @Min(value = 1, message = "La cantidad mínima de niveles es 1")
    @Max(value = 3, message = "La cantidad máxima de niveles es 3")
    private Integer nivelesRedComercial;

    @Size(max = 500, message = "Las observaciones no pueden exceder 500 caracteres")
    private String observaciones;

    public String getClienteNombre() {
        return clienteNombre;
    }

    public void setClienteNombre(String clienteNombre) {
        this.clienteNombre = clienteNombre;
    }

    public BigDecimal getMontoTotal() {
        return montoTotal;
    }

    public void setMontoTotal(BigDecimal montoTotal) {
        this.montoTotal = montoTotal;
    }

    public Long getSocioComercialId() {
        return socioComercialId;
    }

    public void setSocioComercialId(Long socioComercialId) {
        this.socioComercialId = socioComercialId;
    }

    public Integer getNivelesRedComercial() {
        return nivelesRedComercial;
    }

    public void setNivelesRedComercial(Integer nivelesRedComercial) {
        this.nivelesRedComercial = nivelesRedComercial;
    }

    public String getObservaciones() {
        return observaciones;
    }

    public void setObservaciones(String observaciones) {
        this.observaciones = observaciones;
    }
}