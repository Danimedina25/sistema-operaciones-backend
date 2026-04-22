package com.sistemadeoperaciones.pagos.service;

import com.sistemadeoperaciones.pagos.dto.CreateOperationPaymentRequestDto;
import com.sistemadeoperaciones.pagos.dto.CreatePaymentOperationRequestDto;
import com.sistemadeoperaciones.pagos.dto.OperationPaymentResponseDto;
import com.sistemadeoperaciones.pagos.dto.PaymentOperationFilterDto;
import com.sistemadeoperaciones.pagos.dto.PaymentOperationResponseDto;
import com.sistemadeoperaciones.pagos.dto.UpdatePaymentStatusRequestDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface PaymentOperationService {

    PaymentOperationResponseDto createOperation(CreatePaymentOperationRequestDto request);

    OperationPaymentResponseDto addPayment(CreateOperationPaymentRequestDto request);

    OperationPaymentResponseDto validatePayment(Long paymentId, UpdatePaymentStatusRequestDto request);

    OperationPaymentResponseDto rejectPayment(Long paymentId, UpdatePaymentStatusRequestDto request);

    PaymentOperationResponseDto findById(Long id);

    Page<PaymentOperationResponseDto> findAll(PaymentOperationFilterDto filter, Pageable pageable);

    Page<PaymentOperationResponseDto> findAllBySocioComercialId(
            Long socioComercialId,
            PaymentOperationFilterDto filter,
            Pageable pageable
    );

    Page<PaymentOperationResponseDto> findMyOperations(PaymentOperationFilterDto filter, Pageable pageable);

    List<String> findFrequentClientNames();
}