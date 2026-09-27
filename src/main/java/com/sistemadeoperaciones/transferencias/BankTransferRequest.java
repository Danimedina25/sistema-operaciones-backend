package com.sistemadeoperaciones.transferencias;

import java.math.BigDecimal;
import java.util.UUID;

public record BankTransferRequest(
        UUID requestId,
        Long cuentaOrigenId,
        Long cuentaDestinoId,
        BigDecimal monto,
        String referencia,
        String comprobanteUrl
) {}
