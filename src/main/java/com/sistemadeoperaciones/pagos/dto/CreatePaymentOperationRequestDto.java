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

    @NotNull(message = "Los niveles de socios comerciales son obligatorios")
    @Min(value = 1, message = "El nivel mínimo de red comercial es 1")
    @Max(value = 3, message = "El nivel máximo de red comercial es 3")
    private Integer nivelesRedComercial;

    @Size(max = 500, message = "Las observaciones no pueden exceder 500 caracteres")
    private String observaciones;

    @NotNull(message = "El porcentaje de comisión de oficina es obligatorio")
    @DecimalMin(value = "0.00", message = "El porcentaje de comisión de oficina no puede ser negativo")
    private BigDecimal porcentajeComisionOficina;

    @NotNull(message = "El porcentaje de comisión por socio comercial es obligatorio")
    @DecimalMin(value = "0.00", message = "El porcentaje de comisión por socio comercial no puede ser negativo")
    private BigDecimal porcentajeComisionSocio;

    @DecimalMin(value = "0.00", message = "El porcentaje de comisión del socio comercial nivel 2 no puede ser negativo")
    private BigDecimal porcentajeComisionSocioNivel2;

    @DecimalMin(value = "0.00", message = "El porcentaje de comisión del socio comercial nivel 3 no puede ser negativo")
    private BigDecimal porcentajeComisionSocioNivel3;

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

    public BigDecimal getPorcentajeComisionOficina() {
        return porcentajeComisionOficina;
    }

    public void setPorcentajeComisionOficina(BigDecimal porcentajeComisionOficina) {
        this.porcentajeComisionOficina = porcentajeComisionOficina;
    }

    public BigDecimal getPorcentajeComisionSocio() {
        return porcentajeComisionSocio;
    }

    public void setPorcentajeComisionSocio(BigDecimal porcentajeComisionSocio) {
        this.porcentajeComisionSocio = porcentajeComisionSocio;
    }

    public BigDecimal getPorcentajeComisionSocioNivel2() {
        return porcentajeComisionSocioNivel2;
    }

    public void setPorcentajeComisionSocioNivel2(BigDecimal porcentajeComisionSocioNivel2) {
        this.porcentajeComisionSocioNivel2 = porcentajeComisionSocioNivel2;
    }

    public BigDecimal getPorcentajeComisionSocioNivel3() {
        return porcentajeComisionSocioNivel3;
    }

    public void setPorcentajeComisionSocioNivel3(BigDecimal porcentajeComisionSocioNivel3) {
        this.porcentajeComisionSocioNivel3 = porcentajeComisionSocioNivel3;
    }
}