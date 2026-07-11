package com.sistemadeoperaciones.pagos.service;

import com.sistemadeoperaciones.pagos.dto.PaymentOperationFilterDto;
import com.sistemadeoperaciones.pagos.dto.PaymentOperationResponseDto;
import com.sistemadeoperaciones.pagos.dto.retornos.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface ReturnsOperationService {

    /**
     * SOCIO_COMERCIAL:
     * Solicita cómo quiere recibir el retorno.
     */
    List<ReturnPaymentResponseDto> requestReturnPayment(
            Long operationId,
            CreateReturnPaymentBatchRequestDto request
    );

    ReturnPaymentResponseDto updateRequestReturnPayment(
            Long operationId,
            CreateReturnPaymentRequestDto request
    );

    /**
     * JEFA_CAJAS / GERENTE / ADMIN:
     * Marca el retorno como realizado.
     */
    ReturnPaymentResponseDto realizeReturnPayment(
            Long returnPaymentId,
            RealizeReturnPaymentRequestDto request
    );

    /**
     * Historial de retornos de una operación.
     */
    List<ReturnPaymentResponseDto> findReturnsByOperationId(Long operationId);

    /**
     * Detalle general de la operación.
     */
    PaymentOperationResponseDto findReturnDetailByOperationId(Long operationId);

    /**
     * Para SOCIO_COMERCIAL.
     * Operaciones donde el socio puede solicitar retornos.
     */
    Page<PaymentOperationResponseDto> findOperationsAvailableToRequestReturn(
            PaymentOperationFilterDto filter,
            Pageable pageable
    );

    /**
     * Para JEFA_CAJAS / ADMIN / GERENTE.
     * Operaciones con retornos pendientes por realizar.
     */
    Page<PaymentOperationResponseDto> findOperationsWithRequestedReturns(
            PaymentOperationFilterDto filter,
            Pageable pageable
    );

    List<ReturnDestinationAccountSuggestionDto> findReturnDestinationSuggestionsByClienteId(
            Long clienteId
    );

    ReturnPaymentResponseDto scheduleCashReturnPickup(
            Long returnPaymentId,
            ScheduleCashReturnPickupRequestDto request
    );

    /**
     * SOCIO_COMERCIAL (dueño de la operación):
     * Confirma que recibió el retorno en efectivo/retiro sin tarjeta
     * previamente programado por jefa de cajas.
     */
    ReturnPaymentResponseDto confirmCashReturnPickup(Long returnPaymentId);
}