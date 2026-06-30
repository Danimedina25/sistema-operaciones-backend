package com.sistemadeoperaciones.pagos.controller;

import com.sistemadeoperaciones.pagos.dto.PaymentOperationFilterDto;
import com.sistemadeoperaciones.pagos.dto.PaymentOperationResponseDto;
import com.sistemadeoperaciones.pagos.dto.retornos.*;
import com.sistemadeoperaciones.pagos.service.ReturnsOperationService;
import com.sistemadeoperaciones.shared.dto.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/operations/returns")
public class ReturnsOperationController {

    private final ReturnsOperationService returnsOperationService;

    public ReturnsOperationController(ReturnsOperationService returnsOperationService) {
        this.returnsOperationService = returnsOperationService;
    }

    @GetMapping("/available-to-request")
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE', 'SOCIO_COMERCIAL')")
    public ResponseEntity<ApiResponse<Page<PaymentOperationResponseDto>>> findOperationsAvailableToRequestReturn(
            PaymentOperationFilterDto filter,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        Page<PaymentOperationResponseDto> response =
                returnsOperationService.findOperationsAvailableToRequestReturn(filter, pageable);

        return ResponseEntity.ok(
                new ApiResponse<>(
                        true,
                        "Operaciones disponibles para solicitar retorno obtenidas exitosamente",
                        response,
                        null
                )
        );
    }

    @GetMapping("/requested")
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE', 'AUXILIAR_CUENTAS', 'JEFA_CAJAS', 'JEFA_CUENTAS')")
    public ResponseEntity<ApiResponse<Page<PaymentOperationResponseDto>>> findOperationsWithRequestedReturns(
            PaymentOperationFilterDto filter,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        Page<PaymentOperationResponseDto> response =
                returnsOperationService.findOperationsWithRequestedReturns(filter, pageable);

        return ResponseEntity.ok(
                new ApiResponse<>(
                        true,
                        "Operaciones con retornos solicitados obtenidas exitosamente",
                        response,
                        null
                )
        );
    }

    @PostMapping("/{operationId}/request")
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE', 'SOCIO_COMERCIAL')")
    public ResponseEntity<ApiResponse<List<ReturnPaymentResponseDto>>> requestReturnPayment(
            @PathVariable Long operationId,
            @Valid @RequestBody CreateReturnPaymentBatchRequestDto request
    ) {
        List<ReturnPaymentResponseDto> response =
                returnsOperationService.requestReturnPayment(operationId, request);

        return ResponseEntity.status(HttpStatus.CREATED).body(
                new ApiResponse<>(
                        true,
                        "Solicitud de retorno registrada exitosamente",
                        response,
                        null
                )
        );
    }

    @PutMapping("/{returnPaymentId}/update")
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE', 'SOCIO_COMERCIAL')")
    public ResponseEntity<ApiResponse<ReturnPaymentResponseDto>>
    updateRequestReturnPayment(
            @PathVariable Long returnPaymentId,
            @Valid @RequestBody CreateReturnPaymentRequestDto request
    ) {
        ReturnPaymentResponseDto response =
                returnsOperationService.updateRequestReturnPayment(
                        returnPaymentId,
                        request
                );

        return ResponseEntity.ok(
                new ApiResponse<>(
                        true,
                        "Solicitud de retorno actualizada exitosamente",
                        response,
                        null
                )
        );
    }

    @PatchMapping("/payments/{returnPaymentId}/realize")
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE', 'AUXILIAR_CUENTAS', 'JEFA_CAJAS', 'JEFA_CUENTAS')")
    public ResponseEntity<ApiResponse<ReturnPaymentResponseDto>> realizeReturnPayment(
            @PathVariable Long returnPaymentId,
            @Valid @RequestBody RealizeReturnPaymentRequestDto request
    ) {
        ReturnPaymentResponseDto response =
                returnsOperationService.realizeReturnPayment(returnPaymentId, request);

        return ResponseEntity.ok(
                new ApiResponse<>(
                        true,
                        "Retorno realizado exitosamente",
                        response,
                        null
                )
        );
    }

    @GetMapping("/{operationId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE', 'SOCIO_COMERCIAL', 'AUXILIAR_CUENTAS', 'JEFA_CAJAS', 'JEFA_CUENTAS')")
    public ResponseEntity<ApiResponse<PaymentOperationResponseDto>> findReturnDetailByOperationId(
            @PathVariable Long operationId
    ) {
        PaymentOperationResponseDto response =
                returnsOperationService.findReturnDetailByOperationId(operationId);

        return ResponseEntity.ok(
                new ApiResponse<>(
                        true,
                        "Detalle de retorno obtenido exitosamente",
                        response,
                        null
                )
        );
    }

    @GetMapping("/{operationId}/payments")
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE', 'SOCIO_COMERCIAL', 'AUXILIAR_CUENTAS', 'JEFA_CAJAS', 'JEFA_CUENTAS')")
    public ResponseEntity<ApiResponse<List<ReturnPaymentResponseDto>>> findReturnsByOperationId(
            @PathVariable Long operationId
    ) {
        List<ReturnPaymentResponseDto> response =
                returnsOperationService.findReturnsByOperationId(operationId);

        return ResponseEntity.ok(
                new ApiResponse<>(
                        true,
                        "Pagos de retorno obtenidos exitosamente",
                        response,
                        null
                )
        );
    }

    @GetMapping("/clients/{clientId}/destination-accounts")
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE', 'SOCIO_COMERCIAL', 'JEFA_CUENTAS')")
    public ResponseEntity<ApiResponse<List<ReturnDestinationAccountSuggestionDto>>>
    findReturnDestinationAccounts(
            @PathVariable Long clientId
    ) {
        List<ReturnDestinationAccountSuggestionDto> response =
                returnsOperationService.findReturnDestinationSuggestionsByClienteId(
                        clientId
                );

        return ResponseEntity.ok(
                new ApiResponse<>(
                        true,
                        "Cuentas de retorno obtenidas exitosamente",
                        response,
                        null
                )
        );
    }

    @PatchMapping("/payments/{returnPaymentId}/cash-pickup-time")
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE', 'JEFA_CAJAS')")
    public ResponseEntity<ApiResponse<ReturnPaymentResponseDto>> scheduleCashReturnPickup(
            @PathVariable Long returnPaymentId,
            @Valid @RequestBody ScheduleCashReturnPickupRequestDto request
    ) {
        ReturnPaymentResponseDto response =
                returnsOperationService.scheduleCashReturnPickup(
                        returnPaymentId,
                        request
                );

        return ResponseEntity.ok(
                new ApiResponse<>(
                        true,
                        "Fecha y hora de recolección registrada exitosamente",
                        response,
                        null
                )
        );
    }
}