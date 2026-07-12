package com.sistemadeoperaciones.clientes.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class ClienteResponseDto {

    private Long id;
    private String nombre;
    private Boolean activo;
    private Integer nivelesRedComercial;
    private BigDecimal porcentajeComisionSocio;
    private BigDecimal porcentajeComisionOficina;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public ClienteResponseDto() {
    }

    public ClienteResponseDto(
            Long id,
            String nombre,
            Boolean activo,
            Integer nivelesRedComercial,
            BigDecimal porcentajeComisionSocio,
            BigDecimal porcentajeComisionOficina,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
        this.id = id;
        this.nombre = nombre;
        this.activo = activo;
        this.nivelesRedComercial = nivelesRedComercial;
        this.porcentajeComisionSocio = porcentajeComisionSocio;
        this.porcentajeComisionOficina = porcentajeComisionOficina;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public Long getId() {
        return id;
    }

    public String getNombre() {
        return nombre;
    }

    public Boolean getActivo() {
        return activo;
    }

    public Integer getNivelesRedComercial() {
        return nivelesRedComercial;
    }

    public BigDecimal getPorcentajeComisionSocio() {
        return porcentajeComisionSocio;
    }

    public BigDecimal getPorcentajeComisionOficina() {
        return porcentajeComisionOficina;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public void setActivo(Boolean activo) {
        this.activo = activo;
    }

    public void setNivelesRedComercial(Integer nivelesRedComercial) {
        this.nivelesRedComercial = nivelesRedComercial;
    }

    public void setPorcentajeComisionSocio(BigDecimal porcentajeComisionSocio) {
        this.porcentajeComisionSocio = porcentajeComisionSocio;
    }

    public void setPorcentajeComisionOficina(BigDecimal porcentajeComisionOficina) {
        this.porcentajeComisionOficina = porcentajeComisionOficina;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
