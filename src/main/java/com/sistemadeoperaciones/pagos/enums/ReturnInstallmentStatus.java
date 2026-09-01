package com.sistemadeoperaciones.pagos.enums;

/**
 * Estatus de una parcialidad ({@code OperationReturnInstallment}) — un movimiento
 * individual con el que se cubre parte (o todo) de una solicitud de retorno.
 *
 * El cierre de una recolección en efectivo / retiro sin tarjeta tiene dos marcas
 * INDEPENDIENTES: la confirmación del socio comercial ({@code fechaConfirmacion})
 * y el cierre de la jefa de cajas ({@code fechaEntrega}, con foto + persona que
 * recibió). Se registran en cualquier orden.
 *
 * <ul>
 *   <li>{@code PROGRAMADA}  — recolección agendada, ninguna de las dos marcas.
 *       NO cuenta como dinero retornado.</li>
 *   <li>{@code ENTREGADA}   — "confirmación parcial": exactamente UNA de las dos
 *       marcas presente (falta la otra parte). NO cuenta todavía.</li>
 *   <li>{@code COMPLETADA}  — AMBAS marcas presentes. Es la ÚNICA que cuenta como
 *       monto efectivamente retornado; {@code fechaRealizacion} se fija aquí.</li>
 *   <li>{@code CANCELADA}   — cancelada antes de que haya cualquier marca (solo
 *       desde {@code PROGRAMADA}). Libera el saldo que tenía reservado.</li>
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
