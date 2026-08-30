package com.sistemadeoperaciones.pagos.enums;

public enum PaymentStatus {
    PENDIENTE_VALIDACION,
    /**
     * El comprobante ya fue tomado por cuentas pero la transferencia/depósito
     * todavía no se refleja en las cuentas de la empresa. Estado intermedio
     * exclusivo de pagos TRANSFERENCIA/DEPOSITO; desde aquí se puede validar,
     * rechazar o liberar (volver a PENDIENTE_VALIDACION).
     */
    EN_PROCESO,
    VALIDADA,
    RECHAZADA
}