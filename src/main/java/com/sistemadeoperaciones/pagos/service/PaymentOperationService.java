package com.sistemadeoperaciones.pagos.service;

import com.sistemadeoperaciones.pagos.dto.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface PaymentOperationService {

    PaymentOperationResponseDto createOperation(CreatePaymentOperationRequestDto request);

    PaymentOperationResponseDto updateOperation(Long operationId, UpdatePaymentOperationRequestDto request);

    OperationPaymentResponseDto addPayment(CreateOperationPaymentRequestDto request);

    OperationPaymentResponseDto updatePayment(Long paymentId, UpdateOperationPaymentRequestDto request);

    OperationPaymentResponseDto validatePayment(Long paymentId, UpdatePaymentStatusRequestDto request);

    OperationPaymentResponseDto rejectPayment(Long paymentId, UpdatePaymentStatusRequestDto request);

    /**
     * Marca un comprobante TRANSFERENCIA/DEPOSITO como "en proceso" (a la espera
     * de que el movimiento se refleje en las cuentas de la empresa). Solo desde
     * PENDIENTE_VALIDACION.
     */
    OperationPaymentResponseDto markPaymentInProgress(Long paymentId, UpdatePaymentStatusRequestDto request);

    /**
     * Regresa un comprobante "en proceso" a PENDIENTE_VALIDACION.
     */
    OperationPaymentResponseDto releasePaymentInProgress(Long paymentId);

    OperationPaymentResponseDto updateValidationReceipt(Long paymentId, UpdatePaymentStatusRequestDto request);

    PaymentOperationResponseDto findById(Long id);

    Page<PaymentOperationResponseDto> findAll(PaymentOperationFilterDto filter, Pageable pageable);

    Page<PaymentOperationResponseDto> findAllBySocioComercialId(
            Long socioComercialId,
            PaymentOperationFilterDto filter,
            Pageable pageable
    );

    Page<PaymentOperationResponseDto> findMyOperations(PaymentOperationFilterDto filter, Pageable pageable);

    List<String> findFrequentClientNames();

    PaymentOperationResponseDto markAsInvoiced(Long operationId);

    PaymentOperationResponseDto activate(Long id);

    PaymentOperationResponseDto deactivate(Long id);

    /**
     * ADMIN/GERENTE/DIRECCION:
     * Operaciones activas, no finalizadas (excluye COMPLETADA, RETORNADA,
     * RECHAZADA), sin actualizarse desde hace más de `thresholdHours`
     * horas — ordenadas por `updatedAt` ascendente (las más antiguas primero).
     */
    Page<PaymentOperationResponseDto> findStalledOperations(int thresholdHours, Pageable pageable);
}