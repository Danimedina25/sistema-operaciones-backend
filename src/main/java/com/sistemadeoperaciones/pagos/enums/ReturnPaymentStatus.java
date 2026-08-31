package com.sistemadeoperaciones.pagos.enums;

/**
 * Estatus de una solicitud de retorno ({@code OperationReturnPayment}).
 *
 * El backend lo recalcula tras cada cambio de parcialidad a partir de los montos
 * (ver {@code ReturnInstallmentServiceImpl}):
 * <ul>
 *   <li>{@code SOLICITADO}              — sin parcialidades activas ni completadas.</li>
 *   <li>{@code EN_RECOLECCION}          — hay una parcialidad PROGRAMADA (efectivo/retiro
 *       sin tarjeta agendado), ninguna completada. Rol de "en proceso".</li>
 *   <li>{@code ENTREGADO}               — hay una parcialidad ENTREGADA (efectivo entregado,
 *       sin cerrar), ninguna completada.</li>
 *   <li>{@code PARCIALMENTE_RETORNADO}  — al menos una parcialidad completada y todavía
 *       hay saldo pendiente.</li>
 *   <li>{@code RETORNADO}               — la suma de parcialidades completadas cubre el
 *       monto solicitado.</li>
 * </ul>
 */
public enum ReturnPaymentStatus {
    SOLICITADO,
    EN_RECOLECCION,
    ENTREGADO,
    PARCIALMENTE_RETORNADO,
    RETORNADO
}
