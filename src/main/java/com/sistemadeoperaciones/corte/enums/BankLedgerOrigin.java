package com.sistemadeoperaciones.corte.enums;

/** De dónde proviene un movimiento bancario del libro derivado. */
public enum BankLedgerOrigin {
    /** Pago de operación validado hacia la cuenta destino. */
    PAGO,
    /** Parcialidad de retorno completada desde la cuenta origen. */
    RETORNO,
    /** Cheque cobrado o retiro con o sin tarjeta confirmado como entrada en Caja General. */
    CAJA_GENERAL,
    /** Comisión a socio comercial transferida desde la cuenta. */
    COMISION,
    /**
     * Transferencia entre dos cuentas propias. Produce dos renglones —salida en la origen y
     * entrada en la destino— a partir de una sola fila de {@code bank_transfers}.
     */
    TRANSFERENCIA_INTERNA
}
