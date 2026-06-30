package com.sistemadeoperaciones.pagos.dto.retornos;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public class ScheduleCashReturnPickupRequestDto {

    @NotNull(message = "La fecha y hora de recolección es obligatoria")
    private LocalDateTime fechaHoraRecoleccionEfectivo;

    private String observaciones;

    public LocalDateTime getFechaHoraRecoleccionEfectivo() {
        return fechaHoraRecoleccionEfectivo;
    }

    public void setFechaHoraRecoleccionEfectivo(LocalDateTime fechaHoraRecoleccionEfectivo) {
        this.fechaHoraRecoleccionEfectivo = fechaHoraRecoleccionEfectivo;
    }

    public String getObservaciones() {
        return observaciones;
    }

    public void setObservaciones(String observaciones) {
        this.observaciones = observaciones;
    }
}