package com.sistemadeoperaciones.retiroscaja;

import java.math.BigDecimal;
import java.util.UUID;

public record RegisterWithdrawalRequest(
        UUID requestId,
        Long bankAccountId,
        WithdrawalMethod forma,
        BigDecimal monto,
        String referencia,
        String comprobanteUrl
) {}
