package com.sistemadeoperaciones.pagos.service;

import com.sistemadeoperaciones.pagos.dto.PaymentOperationFilterDto;
import com.sistemadeoperaciones.pagos.dto.retornos.CreateReturnPaymentRequestDto;
import com.sistemadeoperaciones.pagos.dto.PaymentOperationResponseDto;
import com.sistemadeoperaciones.pagos.dto.retornos.ReturnPaymentResponseDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface ReturnsOperationService {
    ReturnPaymentResponseDto registerReturnPayment(
            Long operationId,
            CreateReturnPaymentRequestDto request
    );

    List<ReturnPaymentResponseDto> findReturnsByOperationId(Long operationId);

    PaymentOperationResponseDto findReturnDetailByOperationId(Long operationId);

    Page<PaymentOperationResponseDto> findOperationsReadyForReturn(
            PaymentOperationFilterDto filter,
            Pageable pageable
    );
}