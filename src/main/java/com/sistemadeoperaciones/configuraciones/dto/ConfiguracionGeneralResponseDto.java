package com.sistemadeoperaciones.configuraciones.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class ConfiguracionGeneralResponseDto {

    private Long id;
    private BigDecimal porcentajeComisionOficina;
    private LocalDateTime updatedAt;

    public ConfiguracionGeneralResponseDto() {
    }

    public ConfiguracionGeneralResponseDto(Long id, BigDecimal porcentajeComisionOficina, LocalDateTime updatedAt) {
        this.id = id;
        this.porcentajeComisionOficina = porcentajeComisionOficina;
        this.updatedAt = updatedAt;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public BigDecimal getPorcentajeComisionOficina() {
        return porcentajeComisionOficina;
    }

    public void setPorcentajeComisionOficina(BigDecimal porcentajeComisionOficina) {
        this.porcentajeComisionOficina = porcentajeComisionOficina;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
