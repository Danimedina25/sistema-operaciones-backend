package com.sistemadeoperaciones.pagos.enums;

/**
 * Estatus de una parcialidad ({@code OperationReturnInstallment}) — un movimiento
 * individual con el que se cubre parte (o todo) de una solicitud de retorno.
 *
 * <ul>
 *   <li>{@code PROGRAMADA}  — efectivo / retiro sin tarjeta con recolección agendada,
 *       aún sin confirmar. NO cuenta como dinero retornado.</li>
 *   <li>{@code ENTREGADA}   — el socio confirmó haber recibido el efectivo, pendiente
 *       de que la jefa de cajas cierre la entrega. NO cuenta todavía.</li>
 *   <li>{@code COMPLETADA}  — parcialidad confirmada/realizada. Es la ÚNICA que cuenta
 *       como monto efectivamente retornado.</li>
 *   <li>{@code CANCELADA}   — parcialidad cancelada/revertida antes de completarse.
 *       No cuenta y libera el saldo que tenía reservado.</li>
 * </ul>
 *
 * Para transferencia / depósito / cheque la parcialidad nace directamente
 * {@code COMPLETADA} (se registra un movimiento ya realizado, con comprobante).
 */
public enum ReturnInstallmentStatus {
    PROGRAMADA,
    ENTREGADA,
    COMPLETADA,
    CANCELADA
}
