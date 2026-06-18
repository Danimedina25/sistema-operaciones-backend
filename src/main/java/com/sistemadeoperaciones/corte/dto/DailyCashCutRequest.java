package com.sistemadeoperaciones.corte.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public class DailyCashCutRequest {

    private LocalDate fecha;

    /**
     * Solo se usaría si es el primer corte
     * o si administrativamente permiten capturar saldo inicial.
     */
    private BigDecimal saldoInicialManual;

    private String observaciones;

    private Long generadoPorId;

    public DailyCashCutRequest() {
    }

    public LocalDate getFecha() {
        return fecha;
    }

    public void setFecha(LocalDate fecha) {
        this.fecha = fecha;
    }

    public BigDecimal getSaldoInicialManual() {
        return saldoInicialManual;
    }

    public void setSaldoInicialManual(BigDecimal saldoInicialManual) {
        this.saldoInicialManual = saldoInicialManual;
    }

    public String getObservaciones() {
        return observaciones;
    }

    public void setObservaciones(String observaciones) {
        this.observaciones = observaciones;
    }

    public Long getGeneradoPorId() {
        return generadoPorId;
    }

    public void setGeneradoPorId(Long generadoPorId) {
        this.generadoPorId = generadoPorId;
    }
}