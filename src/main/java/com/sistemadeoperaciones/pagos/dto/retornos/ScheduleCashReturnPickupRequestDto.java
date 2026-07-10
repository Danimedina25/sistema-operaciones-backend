package com.sistemadeoperaciones.pagos.dto.retornos;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public class ScheduleCashReturnPickupRequestDto {

    @NotNull(message = "La fecha y hora de recolección es obligatoria")
    private LocalDateTime fechaHoraRecoleccionEfectivo;

    private Long cuentaOrigenId;

    private String observaciones;

    public LocalDateTime getFechaHoraRecoleccionEfectivo() {
        return fechaHoraRecoleccionEfectivo;
    }

    public void setFechaHoraRecoleccionEfectivo(LocalDateTime fechaHoraRecoleccionEfectivo) {
        this.fechaHoraRecoleccionEfectivo = fechaHoraRecoleccionEfectivo;
    }

    public Long getCuentaOrigenId() {
        return cuentaOrigenId;
    }

    public void setCuentaOrigenId(Long cuentaOrigenId) {
        this.cuentaOrigenId = cuentaOrigenId;
    }

    public String getObservaciones() {
        return observaciones;
    }

    public void setObservaciones(String observaciones) {
        this.observaciones = observaciones;
    }
}