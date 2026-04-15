package com.sistemadeoperaciones.pagos.controller;

import com.sistemadeoperaciones.pagos.dto.CreateOperationPaymentRequestDto;
import com.sistemadeoperaciones.pagos.dto.CreatePaymentOperationRequestDto;
import com.sistemadeoperaciones.pagos.dto.OperationPaymentResponseDto;
import com.sistemadeoperaciones.pagos.dto.PaymentOperationResponseDto;
import com.sistemadeoperaciones.pagos.dto.UpdatePaymentStatusRequestDto;
import com.sistemadeoperaciones.pagos.service.PaymentOperationService;
import com.sistemadeoperaciones.shared.dto.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/operations")
public class PaymentOperationController {

    private final PaymentOperationService paymentOperationService;

    public PaymentOperationController(PaymentOperationService paymentOperationService) {
        this.paymentOperationService = paymentOperationService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE', 'SOCIO_COMERCIAL')")
    public ResponseEntity<ApiResponse<PaymentOperationResponseDto>> createOperation(
            @Valid @RequestBody CreatePaymentOperationRequestDto request
    ) {
        PaymentOperationResponseDto response = paymentOperationService.createOperation(request);

        return ResponseEntity.status(HttpStatus.CREATED).body(
                new ApiResponse<>(true, "Operación creada exitosamente", response, null)
        );
    }

    @PostMapping("/payments")
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE', 'SOCIO_COMERCIAL')")
    public ResponseEntity<ApiResponse<OperationPaymentResponseDto>> addPayment(
            @Valid @RequestBody CreateOperationPaymentRequestDto request
    ) {
        OperationPaymentResponseDto response = paymentOperationService.addPayment(request);

        return ResponseEntity.status(HttpStatus.CREATED).body(
                new ApiResponse<>(true, "Pago registrado exitosamente", response, null)
        );
    }

    @PatchMapping("/payments/{paymentId}/validate")
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE', 'JEFA_CAJAS', 'AUXILIAR_CUENTAS')")
    public ResponseEntity<ApiResponse<OperationPaymentResponseDto>> validatePayment(
            @PathVariable Long paymentId,
            @Valid @RequestBody UpdatePaymentStatusRequestDto request
    ) {
        OperationPaymentResponseDto response = paymentOperationService.validatePayment(paymentId, request);

        return ResponseEntity.ok(
                new ApiResponse<>(true, "Pago validado exitosamente", response, null)
        );
    }

    @PatchMapping("/payments/{paymentId}/reject")
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE', 'JEFA_CAJAS', 'AUXILIAR_CUENTAS')")
    public ResponseEntity<ApiResponse<OperationPaymentResponseDto>> rejectPayment(
            @PathVariable Long paymentId,
            @Valid @RequestBody UpdatePaymentStatusRequestDto request
    ) {
        OperationPaymentResponseDto response = paymentOperationService.rejectPayment(paymentId, request);

        return ResponseEntity.ok(
                new ApiResponse<>(true, "Pago rechazado exitosamente", response, null)
        );
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE', 'SOCIO_COMERCIAL', 'JEFA_CAJAS', 'AUXILIAR_CUENTAS')")
    public ResponseEntity<ApiResponse<List<PaymentOperationResponseDto>>> findAll() {
        List<PaymentOperationResponseDto> response = paymentOperationService.findAll();

        return ResponseEntity.ok(
                new ApiResponse<>(true, "Operaciones obtenidas exitosamente", response, null)
        );
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE', 'SOCIO_COMERCIAL', 'JEFA_CAJAS', 'AUXILIAR_CUENTAS')")
    public ResponseEntity<ApiResponse<PaymentOperationResponseDto>> findById(@PathVariable Long id) {
        PaymentOperationResponseDto response = paymentOperationService.findById(id);

        return ResponseEntity.ok(
                new ApiResponse<>(true, "Operación obtenida exitosamente", response, null)
        );
    }

    @GetMapping("/by-commercial-partner/{socioComercialId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE')")
    public ResponseEntity<ApiResponse<List<PaymentOperationResponseDto>>> findAllBySocioComercialId(
            @PathVariable Long socioComercialId
    ) {
        List<PaymentOperationResponseDto> response =
                paymentOperationService.findAllBySocioComercialId(socioComercialId);

        return ResponseEntity.ok(
                new ApiResponse<>(true, "Operaciones del socio comercial obtenidas exitosamente", response, null)
        );
    }

    @GetMapping("/my-operations")
    @PreAuthorize("hasRole('SOCIO_COMERCIAL')")
    public ResponseEntity<ApiResponse<List<PaymentOperationResponseDto>>> findMyOperations() {
        List<PaymentOperationResponseDto> response = paymentOperationService.findMyOperations();

        return ResponseEntity.ok(
                new ApiResponse<>(true, "Mis operaciones obtenidas exitosamente", response, null)
        );
    }
}