package com.sistemadeoperaciones.pagos.controller;

import com.sistemadeoperaciones.pagos.dto.retornos.CancelReturnInstallmentRequestDto;
import com.sistemadeoperaciones.pagos.dto.retornos.CreateReturnInstallmentRequestDto;
import com.sistemadeoperaciones.pagos.dto.retornos.DeliverReturnInstallmentRequestDto;
import com.sistemadeoperaciones.pagos.dto.retornos.ReturnInstallmentResponseDto;
import com.sistemadeoperaciones.pagos.dto.retornos.ReturnRequestSummaryDto;
import com.sistemadeoperaciones.pagos.enums.PaymentType;
import com.sistemadeoperaciones.pagos.service.ReturnInstallmentService;
import com.sistemadeoperaciones.shared.dto.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/**
 * Parcialidades de una solicitud de retorno. Una parcialidad es un pago/entrega
 * ligado explícitamente al id de una solicitud.
 */
@RestController
@RequestMapping("/api/operations/returns")
public class ReturnInstallmentController {

    private final ReturnInstallmentService returnInstallmentService;

    public ReturnInstallmentController(ReturnInstallmentService returnInstallmentService) {
        this.returnInstallmentService = returnInstallmentService;
    }

    @PostMapping("/requests/{returnRequestId}/installments")
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE', 'DIRECCION', 'JEFA_CAJAS', 'JEFA_CUENTAS', 'AUXILIAR_CUENTAS')")
    public ResponseEntity<ApiResponse<ReturnInstallmentResponseDto>> createInstallment(
            @PathVariable Long returnRequestId,
            @Valid @RequestBody CreateReturnInstallmentRequestDto request
    ) {
        ReturnInstallmentResponseDto response =
                returnInstallmentService.createInstallment(returnRequestId, request);

        return ResponseEntity.status(HttpStatus.CREATED).body(
                new ApiResponse<>(true, "Parcialidad registrada exitosamente", response, null)
        );
    }

    @GetMapping("/requests/{returnRequestId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE', 'DIRECCION', 'SOCIO_COMERCIAL', 'AUXILIAR_CUENTAS', 'JEFA_CAJAS', 'JEFA_CUENTAS')")
    public ResponseEntity<ApiResponse<ReturnRequestSummaryDto>> getRequestSummary(
            @PathVariable Long returnRequestId
    ) {
        ReturnRequestSummaryDto response =
                returnInstallmentService.getRequestSummary(returnRequestId);

        return ResponseEntity.ok(
                new ApiResponse<>(true, "Resumen de la solicitud obtenido exitosamente", response, null)
        );
    }

    @GetMapping("/requests/{returnRequestId}/installments")
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE', 'DIRECCION', 'SOCIO_COMERCIAL', 'AUXILIAR_CUENTAS', 'JEFA_CAJAS', 'JEFA_CUENTAS')")
    public ResponseEntity<ApiResponse<List<ReturnInstallmentResponseDto>>> getInstallments(
            @PathVariable Long returnRequestId
    ) {
        List<ReturnInstallmentResponseDto> response =
                returnInstallmentService.findInstallmentsByRequest(returnRequestId);

        return ResponseEntity.ok(
                new ApiResponse<>(true, "Historial de parcialidades obtenido exitosamente", response, null)
        );
    }

    @PatchMapping("/installments/{installmentId}/confirm")
    @PreAuthorize("hasRole('SOCIO_COMERCIAL')")
    public ResponseEntity<ApiResponse<ReturnInstallmentResponseDto>> confirmInstallment(
            @PathVariable Long installmentId
    ) {
        ReturnInstallmentResponseDto response =
                returnInstallmentService.confirmInstallment(installmentId);

        return ResponseEntity.ok(
                new ApiResponse<>(true, "Parcialidad confirmada exitosamente", response, null)
        );
    }

    @PatchMapping("/installments/{installmentId}/deliver")
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE', 'DIRECCION', 'JEFA_CAJAS')")
    public ResponseEntity<ApiResponse<ReturnInstallmentResponseDto>> deliverInstallment(
            @PathVariable Long installmentId,
            @Valid @RequestBody DeliverReturnInstallmentRequestDto request
    ) {
        ReturnInstallmentResponseDto response =
                returnInstallmentService.deliverInstallment(installmentId, request);

        return ResponseEntity.ok(
                new ApiResponse<>(true, "Parcialidad cerrada exitosamente", response, null)
        );
    }

    @PatchMapping("/installments/{installmentId}/cancel")
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE', 'DIRECCION', 'JEFA_CAJAS', 'JEFA_CUENTAS', 'AUXILIAR_CUENTAS')")
    public ResponseEntity<ApiResponse<ReturnInstallmentResponseDto>> cancelInstallment(
            @PathVariable Long installmentId,
            @Valid @RequestBody CancelReturnInstallmentRequestDto request
    ) {
        ReturnInstallmentResponseDto response =
                returnInstallmentService.cancelInstallment(installmentId, request);

        return ResponseEntity.ok(
                new ApiResponse<>(true, "Parcialidad cancelada exitosamente", response, null)
        );
    }

    @GetMapping("/installments/today-deliveries")
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE', 'DIRECCION', 'JEFA_CAJAS')")
    public ResponseEntity<ApiResponse<Page<ReturnInstallmentResponseDto>>> findTodayPickups(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate fecha,
            @RequestParam(required = false) List<PaymentType> tipoPago,
            @PageableDefault(size = 200, sort = "fechaHoraRecoleccion", direction = Sort.Direction.ASC) Pageable pageable
    ) {
        Page<ReturnInstallmentResponseDto> response =
                returnInstallmentService.findTodayPickups(fecha, tipoPago, pageable);

        return ResponseEntity.ok(
                new ApiResponse<>(true, "Entregas del día obtenidas exitosamente", response, null)
        );
    }

    @GetMapping("/installments/late")
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE', 'DIRECCION', 'JEFA_CAJAS')")
    public ResponseEntity<ApiResponse<Page<ReturnInstallmentResponseDto>>> findLatePickups(
            @PageableDefault(size = 20, sort = "fechaHoraRecoleccion", direction = Sort.Direction.ASC) Pageable pageable
    ) {
        Page<ReturnInstallmentResponseDto> response =
                returnInstallmentService.findLatePickups(pageable);

        return ResponseEntity.ok(
                new ApiResponse<>(true, "Parcialidades atrasadas obtenidas exitosamente", response, null)
        );
    }
}
