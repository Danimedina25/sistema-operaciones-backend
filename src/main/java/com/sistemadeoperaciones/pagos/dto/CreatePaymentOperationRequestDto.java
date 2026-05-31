package com.sistemadeoperaciones.pagos.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public class CreatePaymentOperationRequestDto {

    @NotNull(message = "El cliente es obligatorio")
    private Long clienteId;

    @NotNull(message = "El monto total es obligatorio")
    @DecimalMin(value = "0.01", message = "El monto total debe ser mayor a cero")
    private BigDecimal montoTotal;

    @NotNull(message = "El socio comercial es obligatorio")
    private Long socioComercialId;

    private Long socioComercialNivel2Id;

    private Long socioComercialNivel3Id;


    @Size(max = 500, message = "Las observaciones no pueden exceder 500 caracteres")
    private String observaciones;

    public Long getClienteId() {
        return clienteId;
    }

    public void setClienteId(Long clienteId) {
        this.clienteId = clienteId;
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

    public Long getSocioComercialNivel2Id() {
        return socioComercialNivel2Id;
    }

    public void setSocioComercialNivel2Id(Long socioComercialNivel2Id) {
        this.socioComercialNivel2Id = socioComercialNivel2Id;
    }

    public Long getSocioComercialNivel3Id() {
        return socioComercialNivel3Id;
    }

    public void setSocioComercialNivel3Id(Long socioComercialNivel3Id) {
        this.socioComercialNivel3Id = socioComercialNivel3Id;
    }

    public String getObservaciones() {
        return observaciones;
    }

    public void setObservaciones(String observaciones) {
        this.observaciones = observaciones;
    }
}