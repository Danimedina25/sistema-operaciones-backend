package com.sistemadeoperaciones.corte.dto;

import java.math.BigDecimal;

/**
 * Los mismos movimientos del libro, agrupados en los conceptos que persiste
 * {@code bank_account_daily_cuts}. Es la única definición contable que consumen
 * tanto el saldo en vivo como el corte registrado.
 */
public record BankLedgerBuckets(
        BigDecimal entradasTransferencia,
        BigDecimal entradasDeposito,
        BigDecimal entradasCheque,
        BigDecimal salidasRetornos,
        /** Efectivo retirado del banco hacia la caja: cheque cobrado y retiro sin tarjeta. */
        BigDecimal salidasCajaGeneral
) {
    public BigDecimal totalEntradas() {
        return entradasTransferencia.add(entradasDeposito).add(entradasCheque);
    }

    public BigDecimal totalSalidas() {
        return salidasRetornos.add(salidasCajaGeneral);
    }
}
