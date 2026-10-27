package com.sistemadeoperaciones.retiroscaja;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record BankCashWithdrawalView(
        Long id,
        LocalDateTime registradoEn,
        Long bankAccountId,
        String cuenta,
        String cuentaTitular,
        String cuentaNumero,
        /** Banco agrupado, el mismo que usa Caja General: "SCOTIABANK" para "SCOTIA NOMINA". */
        String banco,
        WithdrawalMethod forma,
        String formaEtiqueta,
        BigDecimal monto,
        String referencia,
        String comprobanteUrl,
        WithdrawalStatus estatus,
        String motivo,
        Long registradoPorId,
        String registradoPorNombre,
        String resueltoPorNombre,
        LocalDateTime resueltoEn,
        Long cashMovementId,
        Long version
) {}
