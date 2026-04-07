package com.sistemadeoperaciones.pagos.service;

import com.sistemadeoperaciones.pagos.dto.CreateOperationPaymentRequestDto;
import com.sistemadeoperaciones.pagos.dto.CreatePaymentOperationRequestDto;
import com.sistemadeoperaciones.pagos.dto.OperationPaymentResponseDto;
import com.sistemadeoperaciones.pagos.dto.PaymentOperationResponseDto;
import com.sistemadeoperaciones.pagos.dto.UpdatePaymentStatusRequestDto;

public interface PaymentOperationService {

    PaymentOperationResponseDto createOperation(CreatePaymentOperationRequestDto request);

    OperationPaymentResponseDto addPayment(CreateOperationPaymentRequestDto request);

    OperationPaymentResponseDto validatePayment(Long paymentId, UpdatePaymentStatusRequestDto request);

    OperationPaymentResponseDto rejectPayment(Long paymentId, UpdatePaymentStatusRequestDto request);

    PaymentOperationResponseDto findById(Long id);
}