package com.sistemadeoperaciones.corte.service;

import com.sistemadeoperaciones.corte.dto.DailyCashCutRequest;
import com.sistemadeoperaciones.corte.dto.DailyCashCutResponse;
import com.sistemadeoperaciones.corte.dto.CashCutRangeResponse;

import java.time.LocalDate;

public interface DailyCashCutService {

    /**
     * Calcula el corte de un día sin guardar nada.
     * Sirve para mostrar el corte en vivo del día actual
     * o para previsualizar un corte antes de registrarlo.
     */
    DailyCashCutResponse calculateDailyCut(LocalDate fecha);

    /**
     * Calcula y registra el snapshot del corte diario en la tabla daily_cash_cuts.
     * Normalmente se usaría desde el scheduler automático.
     */
    DailyCashCutResponse registerDailyCut(LocalDate fecha);

    /**
     * Calcula y registra el corte diario permitiendo datos adicionales,
     * como observaciones o un saldo inicial manual para el primer corte.
     */
    DailyCashCutResponse registerDailyCut(DailyCashCutRequest request);

    /**
     * Consulta un resumen por rango de fechas sin guardar nada.
     * Puede usarse para cortes semanales, mensuales, anuales o personalizados.
     */
    CashCutRangeResponse calculateRangeCut(LocalDate fechaInicio, LocalDate fechaFin);
}