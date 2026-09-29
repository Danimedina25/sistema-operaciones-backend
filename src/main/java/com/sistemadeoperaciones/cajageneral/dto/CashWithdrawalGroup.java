package com.sistemadeoperaciones.cajageneral.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Renglón agrupado del libro de Caja General, como lo lleva negocio en su Excel:
 * "Retiro 16 de 16 TD BANORTE", "Retiro 9 de 9 RST SCOTIABANK", "Cheques BAJIO".
 *
 * <p>Es sólo presentación: cada retiro sigue siendo su propio movimiento, con su desglose y
 * su salida bancaria. El grupo junta los del mismo día, forma y banco.
 *
 * @param forma        {@code TD}, {@code RST} o {@code CHEQUE}
 * @param cuentas      N: cuentas distintas de ese banco retiradas ese día (null en cheques)
 * @param totalCuentas M: cuentas activas de ese banco (null en cheques)
 * @param ultimoMovimientoId el renglón se muestra en la posición y con el saldo de este movimiento
 */
public record CashWithdrawalGroup(
        Long diaId,
        LocalDate fecha,
        String forma,
        String banco,
        Integer cuentas,
        Integer totalCuentas,
        int movimientos,
        BigDecimal total,
        List<Long> movementIds,
        Long ultimoMovimientoId,
        String etiqueta
) {}
