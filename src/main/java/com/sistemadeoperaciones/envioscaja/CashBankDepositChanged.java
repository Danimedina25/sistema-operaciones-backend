package com.sistemadeoperaciones.envioscaja;

/** Notifica a la otra área después de confirmar la transacción. */
public record CashBankDepositChanged(Long id, CashBankDepositStatus status, String actorName) {}
