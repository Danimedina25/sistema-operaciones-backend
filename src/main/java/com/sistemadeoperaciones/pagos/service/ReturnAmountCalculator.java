package com.sistemadeoperaciones.pagos.service;

import com.sistemadeoperaciones.pagos.model.PaymentOperation;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Centraliza el cálculo del monto que debe retornarse al cliente de una
 * operación:  monto validado − comisión de la red comercial (N1+N2+N3) −
 * comisión de oficina.  Aritmética en {@link BigDecimal}, escala 2, HALF_UP.
 *
 * Lo usan tanto {@code ReturnsOperationServiceImpl} (tope al solicitar) como
 * {@code ReturnInstallmentServiceImpl} (recálculo del estatus de la operación),
 * para tener una única fuente de verdad.
 */
@Component
public class ReturnAmountCalculator {

    public BigDecimal amountToReturn(PaymentOperation operation) {
        BigDecimal montoValidado = safe(operation.getMontoValidado());

        BigDecimal porcentajeRed = safe(operation.getPorcentajeComisionSocio())
                .add(safe(operation.getPorcentajeComisionSocioNivel2()))
                .add(safe(operation.getPorcentajeComisionSocioNivel3()))
                .setScale(2, RoundingMode.HALF_UP);

        BigDecimal comisionRed = amountFromPercentage(montoValidado, porcentajeRed);
        BigDecimal comisionOficina = amountFromPercentage(
                montoValidado,
                operation.getPorcentajeComisionOficina()
        );

        return montoValidado
                .subtract(comisionRed)
                .subtract(comisionOficina)
                .setScale(2, RoundingMode.HALF_UP);
    }

    public BigDecimal amountFromPercentage(BigDecimal base, BigDecimal percentage) {
        return safe(base)
                .multiply(safe(percentage))
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
    }

    public BigDecimal safe(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }
}
