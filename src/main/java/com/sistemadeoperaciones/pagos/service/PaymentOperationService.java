package com.sistemadeoperaciones.pagos.service;

import com.sistemadeoperaciones.pagos.dto.CreateOperationPaymentRequestDto;
import com.sistemadeoperaciones.pagos.dto.CreatePaymentOperationRequestDto;
import com.sistemadeoperaciones.pagos.dto.OperationPaymentResponseDto;
import com.sistemadeoperaciones.pagos.dto.PaymentOperationResponseDto;
import com.sistemadeoperaciones.pagos.dto.UpdatePaymentStatusRequestDto;

import java.util.List;

public interface PaymentOperationService {

    PaymentOperationResponseDto createOperation(CreatePaymentOperationRequestDto request);

    OperationPaymentResponseDto addPayment(CreateOperationPaymentRequestDto request);

    OperationPaymentResponseDto validatePayment(Long paymentId, UpdatePaymentStatusRequestDto request);

    OperationPaymentResponseDto rejectPayment(Long paymentId, UpdatePaymentStatusRequestDto request);

    PaymentOperationResponseDto findById(Long id);

    List<PaymentOperationResponseDto> findAll();

    List<PaymentOperationResponseDto> findAllBySocioComercialId(Long socioComercialId);

    List<PaymentOperationResponseDto> findMyOperations();
    List<String> findFrequentClientNames();
}