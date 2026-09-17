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

    /**
     * Recalcula los cortes ya registrados desde una fecha en adelante y vuelve a encadenar
     * sus saldos, devolviendo cuántos se rehicieron.
     *
     * <p>Hace falta cuando cambia la definición contable del corte —por ejemplo al dejar de
     * contar el efectivo, que ahora vive sólo en Caja General—: los cortes guardados
     * conservarían la fórmula vieja y el saldo inicial de hoy seguiría arrastrándola.
     *
     * <p>Es determinista e idempotente: los importes se recalculan siempre desde los pagos,
     * retornos y cheques originales, nunca desde los agregados guardados. Lo único que se
     * respeta es el saldo inicial del primer corte de la serie, que es un dato de negocio
     * capturado a mano.
     */
    int recalculateFrom(LocalDate desde);
}