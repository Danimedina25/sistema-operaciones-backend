package com.sistemadeoperaciones.pagos.service;

import com.sistemadeoperaciones.pagos.dto.retornos.ReturnPaymentResponseDto;
import com.sistemadeoperaciones.pagos.enums.ReturnInstallmentStatus;
import com.sistemadeoperaciones.pagos.model.OperationReturnPayment;
import com.sistemadeoperaciones.pagos.repository.OperationReturnInstallmentRepository;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Totales calculados de una solicitud de retorno a partir de sus parcialidades.
 * Única fuente de verdad, usada por {@code ReturnsOperationServiceImpl} y
 * {@code ReturnInstallmentServiceImpl}.
 */
@Component
public class ReturnRequestTotalsCalculator {

    private final OperationReturnInstallmentRepository installmentRepository;

    public ReturnRequestTotalsCalculator(OperationReturnInstallmentRepository installmentRepository) {
        this.installmentRepository = installmentRepository;
    }

    public void apply(ReturnPaymentResponseDto dto, OperationReturnPayment solicitud) {
        Long id = solicitud.getId();

        BigDecimal montoSolicitado = scaled(solicitud.getMonto());
        BigDecimal retornado = scaled(installmentRepository.sumCompletedBySolicitud(id));
        BigDecimal enProceso = scaled(installmentRepository.sumInFlightBySolicitud(id));
        BigDecimal pendiente = montoSolicitado.subtract(retornado).max(BigDecimal.ZERO);
        BigDecimal disponible = montoSolicitado.subtract(retornado).subtract(enProceso).max(BigDecimal.ZERO);

        BigDecimal porcentaje = montoSolicitado.compareTo(BigDecimal.ZERO) > 0
                ? retornado.multiply(BigDecimal.valueOf(100))
                    .divide(montoSolicitado, 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        dto.setMontoSolicitado(montoSolicitado);
        dto.setMontoRetornado(retornado);
        dto.setMontoEnProceso(enProceso);
        dto.setMontoPendiente(pendiente);
        dto.setMontoDisponible(disponible);
        dto.setPorcentajeAvance(porcentaje);
        dto.setNumeroParcialidades(installmentRepository.countBySolicitudIdAndEstatusNot(
                id, ReturnInstallmentStatus.CANCELADA));
    }

    private BigDecimal scaled(BigDecimal value) {
        return (value != null ? value : BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
    }
}
