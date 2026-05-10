package com.sistemadeoperaciones.pagos.controller;

import com.sistemadeoperaciones.pagos.dto.PaymentOperationFilterDto;
import com.sistemadeoperaciones.pagos.dto.PaymentOperationResponseDto;
import com.sistemadeoperaciones.pagos.dto.retornos.CreateReturnPaymentRequestDto;
import com.sistemadeoperaciones.pagos.dto.retornos.ReturnPaymentResponseDto;
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

    @GetMapping("/ready")
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE', 'AUXILIAR_CUENTAS')")
    public ResponseEntity<ApiResponse<Page<PaymentOperationResponseDto>>> findOperationsReadyForReturn(
            PaymentOperationFilterDto filter,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        Page<PaymentOperationResponseDto> response =
                returnsOperationService.findOperationsReadyForReturn(filter, pageable);

        return ResponseEntity.ok(
                new ApiResponse<>(
                        true,
                        "Operaciones listas para retorno obtenidas exitosamente",
                        response,
                        null
                )
        );
    }

    @PostMapping("/{operationId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE', 'AUXILIAR_CUENTAS')")
    public ResponseEntity<ApiResponse<ReturnPaymentResponseDto>> registerReturnPayment(
            @PathVariable Long operationId,
            @Valid @RequestBody CreateReturnPaymentRequestDto request
    ) {
        ReturnPaymentResponseDto response =
                returnsOperationService.registerReturnPayment(operationId, request);

        return ResponseEntity.status(HttpStatus.CREATED).body(
                new ApiResponse<>(
                        true,
                        "Retorno registrado exitosamente",
                        response,
                        null
                )
        );
    }

    @GetMapping("/{operationId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE', 'AUXILIAR_CUENTAS')")
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
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE', 'AUXILIAR_CUENTAS')")
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
}