package com.sistemadeoperaciones.configuraciones.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public class ConfiguracionGeneralRequestDTO {

    @NotNull(message = "El porcentaje de comisión de oficina es obligatorio")
    @DecimalMin(value = "0.00", message = "El porcentaje de comisión de oficina no puede ser negativo")
    @DecimalMax(value = "100.00", message = "El porcentaje de comisión de oficina no puede ser mayor a 100")
    private BigDecimal porcentajeComisionOficina;

    public ConfiguracionGeneralRequestDTO() {
    }

    public BigDecimal getPorcentajeComisionOficina() {
        return porcentajeComisionOficina;
    }

    public void setPorcentajeComisionOficina(BigDecimal porcentajeComisionOficina) {
        this.porcentajeComisionOficina = porcentajeComisionOficina;
    }
}
