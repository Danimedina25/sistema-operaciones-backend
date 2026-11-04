package com.sistemadeoperaciones.configuraciones.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class ConfiguracionGeneralResponseDto {

    private Long id;
    private BigDecimal porcentajeComisionOficina;
    private Integer horasPendienteAtencion;
    private Integer horasPendienteUrgente;
    private Integer horasOperacionDetenida;
    private Integer diaPagoComisiones;
    private LocalDateTime updatedAt;

    public ConfiguracionGeneralResponseDto() {
    }

    public ConfiguracionGeneralResponseDto(Long id, BigDecimal porcentajeComisionOficina,
                                           Integer horasPendienteAtencion, Integer horasPendienteUrgente,
                                           Integer horasOperacionDetenida, Integer diaPagoComisiones,
                                           LocalDateTime updatedAt) {
        this.horasOperacionDetenida = horasOperacionDetenida;
        this.diaPagoComisiones = diaPagoComisiones;
        this.id = id;
        this.porcentajeComisionOficina = porcentajeComisionOficina;
        this.horasPendienteAtencion = horasPendienteAtencion;
        this.horasPendienteUrgente = horasPendienteUrgente;
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

    public Integer getHorasPendienteAtencion() {
        return horasPendienteAtencion;
    }

    public void setHorasPendienteAtencion(Integer horasPendienteAtencion) {
        this.horasPendienteAtencion = horasPendienteAtencion;
    }

    public Integer getHorasPendienteUrgente() {
        return horasPendienteUrgente;
    }

    public void setHorasPendienteUrgente(Integer horasPendienteUrgente) {
        this.horasPendienteUrgente = horasPendienteUrgente;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Integer getHorasOperacionDetenida() {
        return horasOperacionDetenida;
    }

    public void setHorasOperacionDetenida(Integer horasOperacionDetenida) {
        this.horasOperacionDetenida = horasOperacionDetenida;
    }

    public Integer getDiaPagoComisiones() {
        return diaPagoComisiones;
    }

    public void setDiaPagoComisiones(Integer diaPagoComisiones) {
        this.diaPagoComisiones = diaPagoComisiones;
    }
}
