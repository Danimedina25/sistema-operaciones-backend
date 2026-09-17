package com.sistemadeoperaciones.cajageneral.enums;
// Describe el origen/destino del efectivo, no crea otro pago bancario.
// Capturables en Caja General: EFECTIVO, CHEQUE y RETIRO_SIN_TARJETA.
// RETIRO_CON_TARJETA nunca existió en la operación: se conserva sólo para poder leer
// movimientos históricos que lo usaron. Los demás valores describen pagos bancarios.
public enum CashMovementConcept { EFECTIVO, CHEQUE, TRANSFERENCIA, DEPOSITO, RETIRO_CON_TARJETA, RETIRO_SIN_TARJETA }
