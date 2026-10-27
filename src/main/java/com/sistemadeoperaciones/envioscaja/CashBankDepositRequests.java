package com.sistemadeoperaciones.envioscaja;

import com.sistemadeoperaciones.cajageneral.enums.CashDenomination;
import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

public final class CashBankDepositRequests {
    private CashBankDepositRequests() {}
    public record Register(UUID requestId, Long bankAccountId, BigDecimal monto, String forma,
                           String referencia, String comprobanteSalidaUrl,
                           Map<CashDenomination, Integer> denominaciones) {}
    public record Edit(Long version, Long bankAccountId, BigDecimal monto, String forma,
                       String referencia, String comprobanteSalidaUrl,
                       Map<CashDenomination, Integer> denominaciones) {}
    public record Confirm(String comprobanteBancoUrl) {}
    public record Reject(String motivo) {}
    public record ConfirmReturn(Map<CashDenomination, Integer> denominaciones) {}
}
