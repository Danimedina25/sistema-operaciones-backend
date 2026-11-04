package com.sistemadeoperaciones.configuraciones.model;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "configuracion_general")
public class ConfiguracionGeneral {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "porcentaje_comision_oficina", nullable = false, precision = 5, scale = 2)
    private BigDecimal porcentajeComisionOficina = new BigDecimal("1.50");

    /** Desde estas horas sin atender, un pendiente se marca en amarillo ("Atender pronto"). */
    @Column(name = "horas_pendiente_atencion", nullable = false)
    private Integer horasPendienteAtencion = 4;

    /** Desde estas horas sin atender, un pendiente se marca en rojo ("Urgente"). */
    @Column(name = "horas_pendiente_urgente", nullable = false)
    private Integer horasPendienteUrgente = 24;

    /** Una operación sin cambios por más de estas horas se considera detenida. */
    @Column(name = "horas_operacion_detenida", nullable = false)
    private Integer horasOperacionDetenida = 48;

    /** Día de la semana en que se pagan las comisiones de la semana anterior (ISO: 1 = lunes … 7 = domingo). */
    @Column(name = "dia_pago_comisiones", nullable = false)
    private Integer diaPagoComisiones = 6;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public ConfiguracionGeneral() {
    }

    @PrePersist
    @PreUpdate
    public void onSave() {
        updatedAt = LocalDateTime.now();
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

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
