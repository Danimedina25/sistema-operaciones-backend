package com.sistemadeoperaciones.cajageneral.enums;
// Describe el origen/destino del efectivo, no crea otro pago bancario.
// Captura manual en Caja General: sólo EFECTIVO. CHEQUE, RETIRO_SIN_TARJETA y
// RETIRO_CON_TARJETA entran al confirmar un retiro que registró Cuentas, siempre con la cuenta
// de la que salió el dinero. COBRO_CHEQUE_CLIENTE lo registra el módulo de cheques.
// TRANSFERENCIA y DEPOSITO describen pagos bancarios y nunca entran a la caja.
public enum CashMovementConcept { COBRO_CHEQUE_CLIENTE, EFECTIVO, CHEQUE, TRANSFERENCIA, DEPOSITO, RETIRO_CON_TARJETA, RETIRO_SIN_TARJETA }
