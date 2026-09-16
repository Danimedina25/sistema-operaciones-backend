package com.sistemadeoperaciones.corte.dto;

import java.math.BigDecimal;

/**
 * Totales calculados sobre TODOS los movimientos que cumplen el filtro, nunca sobre
 * la página consultada.
 */
public record BankLedgerTotalsDto(
        BigDecimal totalEntradas,
        BigDecimal totalSalidas,
        BigDecimal variacionNeta,
        long totalMovimientos
) {
}
