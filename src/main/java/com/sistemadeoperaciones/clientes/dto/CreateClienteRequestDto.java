package com.sistemadeoperaciones.clientes.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public class CreateClienteRequestDto {

    @NotBlank(message = "El nombre del cliente es obligatorio")
    private String nombre;

    @NotNull(message = "Los niveles de red comercial son obligatorios")
    @Min(value = 1, message = "El nivel mínimo de red comercial es 1")
    @Max(value = 3, message = "El nivel máximo de red comercial es 3")
    private Integer nivelesRedComercial;

    @NotNull(message = "El porcentaje de comisión es obligatorio")
    @DecimalMin(value = "0.00", message = "El porcentaje de comisión no puede ser negativo")
    private BigDecimal porcentajeComisionAplicado;

    @NotNull(message = "El ID del usuario es obligatorio")
    private Long userId;

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public Integer getNivelesRedComercial() {
        return nivelesRedComercial;
    }

    public void setNivelesRedComercial(Integer nivelesRedComercial) {
        this.nivelesRedComercial = nivelesRedComercial;
    }

    public BigDecimal getPorcentajeComisionAplicado() {
        return porcentajeComisionAplicado;
    }

    public void setPorcentajeComisionAplicado(BigDecimal porcentajeComisionAplicado) {
        this.porcentajeComisionAplicado = porcentajeComisionAplicado;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }
}