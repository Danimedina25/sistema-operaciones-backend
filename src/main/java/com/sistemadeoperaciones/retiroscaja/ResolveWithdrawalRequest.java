package com.sistemadeoperaciones.retiroscaja;

/** Rechazo (Cajas) o cancelación (Cuentas): siempre con motivo. */
public record ResolveWithdrawalRequest(String motivo) {}
