package com.sistemadeoperaciones.envioscaja;

import com.sistemadeoperaciones.cajageneral.enums.CashDenomination;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

public record CashBankDepositView(Long id, CashBankDepositOrigin origen, Long bankAccountId,
        String cuenta, String cuentaTitular, BigDecimal monto, String forma, String referencia,
        String comprobanteSalidaUrl, String comprobanteBancoUrl, CashBankDepositStatus estatus,
        String motivo, LocalDateTime registradoEn, String registradoPorNombre,
        LocalDateTime resueltoEn, String resueltoPorNombre, Long cashMovementId,
        Long chequePaymentId, Long operationId, String numeroCheque,
        Long version, Map<CashDenomination, Integer> denominaciones) {}
