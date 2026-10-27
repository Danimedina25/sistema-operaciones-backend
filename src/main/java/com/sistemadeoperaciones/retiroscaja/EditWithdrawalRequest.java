package com.sistemadeoperaciones.retiroscaja;

import java.math.BigDecimal;

public record EditWithdrawalRequest(Long version, Long bankAccountId, WithdrawalMethod forma,
                                    BigDecimal monto, String referencia, String comprobanteUrl) {}
