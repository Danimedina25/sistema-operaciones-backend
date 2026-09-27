package com.sistemadeoperaciones.transferencias;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Una transferencia entre cuentas propias. {@code saldoOrigenResultante} sólo se llena al
 * registrarla: sirve para avisar si la cuenta origen quedó en negativo, sin bloquearla,
 * porque el saldo derivado puede no incluir movimientos históricos sin cuenta vinculada.
 */
public record BankTransferView(
        Long id,
        LocalDateTime fecha,
        Long cuentaOrigenId,
        String cuentaOrigen,
        Long cuentaDestinoId,
        String cuentaDestino,
        BigDecimal monto,
        String referencia,
        String comprobanteUrl,
        Long registradoPorId,
        String registradoPorNombre,
        BigDecimal saldoOrigenResultante
) {}
