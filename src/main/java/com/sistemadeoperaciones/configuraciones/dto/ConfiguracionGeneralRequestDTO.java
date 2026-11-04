package com.sistemadeoperaciones.configuraciones.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public class ConfiguracionGeneralRequestDTO {

    @NotNull(message = "El porcentaje de comisión de oficina es obligatorio")
    @DecimalMin(value = "0.00", message = "El porcentaje de comisión de oficina no puede ser negativo")
    @DecimalMax(value = "100.00", message = "El porcentaje de comisión de oficina no puede ser mayor a 100")
    private BigDecimal porcentajeComisionOficina;

    /** Opcional: si no se envía se conserva el valor actual. */
    @Min(value = 1, message = "Las horas para atender pronto deben ser al menos 1")
    @Max(value = 720, message = "Las horas para atender pronto no pueden superar 720")
    private Integer horasPendienteAtencion;

    /** Opcional: si no se envía se conserva el valor actual. */
    @Min(value = 1, message = "Las horas para urgente deben ser al menos 1")
    @Max(value = 720, message = "Las horas para urgente no pueden superar 720")
    private Integer horasPendienteUrgente;

    /** Opcional: si no se envía se conserva el valor actual. */
    @Min(value = 1, message = "Las horas para operación detenida deben ser al menos 1")
    @Max(value = 720, message = "Las horas para operación detenida no pueden superar 720")
    private Integer horasOperacionDetenida;

    /** Opcional: si no se envía se conserva el valor actual. ISO: 1 = lunes … 7 = domingo. */
    @Min(value = 1, message = "El día de pago de comisiones no es válido")
    @Max(value = 7, message = "El día de pago de comisiones no es válido")
    private Integer diaPagoComisiones;

    public ConfiguracionGeneralRequestDTO() {
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
}
