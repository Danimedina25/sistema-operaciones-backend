package com.sistemadeoperaciones.retiroscaja;

/**
 * PENDIENTE: registrado por Cuentas, el efectivo va en camino ("en tránsito") y el saldo
 * bancario todavía no baja. CONFIRMADO: Cajas recibió y contó el efectivo; en ese momento
 * entra a la caja y sale del banco. RECHAZADO lo decide Cajas; CANCELADO, Cuentas.
 */
public enum WithdrawalStatus { PENDIENTE, CONFIRMADO, RECHAZADO, CANCELADO }
