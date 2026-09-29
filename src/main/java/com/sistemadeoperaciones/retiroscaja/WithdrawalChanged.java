package com.sistemadeoperaciones.retiroscaja;

/** Se publica al confirmar la transacción para avisar a la otra área. */
public record WithdrawalChanged(Long withdrawalId, WithdrawalStatus estatus, String actorName) {}
