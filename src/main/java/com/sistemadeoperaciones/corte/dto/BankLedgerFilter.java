package com.sistemadeoperaciones.corte.dto;

import com.sistemadeoperaciones.corte.enums.BankLedgerDirection;

import java.time.LocalDate;

/**
 * Filtros de consulta del libro bancario. {@code desde} y {@code hasta} son obligatorios
 * y se validan en el servicio: rango ordenado, no futuro y de máximo un año.
 */
public record BankLedgerFilter(
        LocalDate desde,
        LocalDate hasta,
        Long bankAccountId,
        String banco,
        BankLedgerDirection direccion,
        String tipo
) {
}
