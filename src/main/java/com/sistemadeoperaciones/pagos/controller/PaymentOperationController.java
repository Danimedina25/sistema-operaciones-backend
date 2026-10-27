package com.sistemadeoperaciones.pagos.controller;

import com.sistemadeoperaciones.pagos.dto.*;
import com.sistemadeoperaciones.pagos.service.PaymentOperationService;
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
@RequestMapping("/api/operations")
public class PaymentOperationController {

    private final PaymentOperationService paymentOperationService;

    public PaymentOperationController(PaymentOperationService paymentOperationService) {
        this.paymentOperationService = paymentOperationService;
    }

    @PostMapping
    @PreAuthorize("@rolePermissionPolicy.allows(authentication, 'OPERATION_CREATE')")
    public ResponseEntity<ApiResponse<PaymentOperationResponseDto>> createOperation(
            @Valid @RequestBody CreatePaymentOperationRequestDto request
    ) {
        PaymentOperationResponseDto response = paymentOperationService.createOperation(request);

        return ResponseEntity.status(HttpStatus.CREATED).body(
                new ApiResponse<>(true, "Operación creada exitosamente", response, null)
        );
    }

    @PutMapping("/{operationId}")
    @PreAuthorize("@rolePermissionPolicy.allows(authentication, 'OPERATION_EDIT')")
    public ResponseEntity<ApiResponse<PaymentOperationResponseDto>> updateOperation(
            @PathVariable Long operationId,
            @Valid @RequestBody UpdatePaymentOperationRequestDto request
    ) {
        PaymentOperationResponseDto response =
                paymentOperationService.updateOperation(operationId, request);

        return ResponseEntity.ok(
                new ApiResponse<>(true, "Operación actualizada exitosamente", response, null)
        );
    }

    @PostMapping("/payments")
    @PreAuthorize("@rolePermissionPolicy.allows(authentication, 'PAYMENT_REGISTER')")
    public ResponseEntity<ApiResponse<OperationPaymentResponseDto>> addPayment(
            @Valid @RequestBody CreateOperationPaymentRequestDto request
    ) {
        OperationPaymentResponseDto response = paymentOperationService.addPayment(request);

        return ResponseEntity.status(HttpStatus.CREATED).body(
                new ApiResponse<>(true, "Pago registrado exitosamente", response, null)
        );
    }

    @PutMapping("/payments/{paymentId}")
    @PreAuthorize("@rolePermissionPolicy.allows(authentication, 'PAYMENT_REGISTER')")
    public ResponseEntity<ApiResponse<OperationPaymentResponseDto>> updatePayment(
            @PathVariable Long paymentId,
            @Valid @RequestBody UpdateOperationPaymentRequestDto request
    ) {
        OperationPaymentResponseDto response =
                paymentOperationService.updatePayment(paymentId, request);

        return ResponseEntity.ok(
                new ApiResponse<>(true, "Pago actualizado exitosamente", response, null)
        );
    }

    @PatchMapping("/payments/{paymentId}/validate")
    @PreAuthorize("@rolePermissionPolicy.allows(authentication, 'PAYMENT_VALIDATE')")
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
    @PreAuthorize("@rolePermissionPolicy.allows(authentication, 'PAYMENT_VALIDATE')")
    public ResponseEntity<ApiResponse<OperationPaymentResponseDto>> rejectPayment(
            @PathVariable Long paymentId,
            @Valid @RequestBody UpdatePaymentStatusRequestDto request
    ) {
        OperationPaymentResponseDto response = paymentOperationService.rejectPayment(paymentId, request);

        return ResponseEntity.ok(
                new ApiResponse<>(true, "Pago rechazado exitosamente", response, null)
        );
    }

    @PatchMapping("/payments/{paymentId}/validation-receipt")
    @PreAuthorize("@rolePermissionPolicy.allows(authentication, 'PAYMENT_VALIDATE')")
    public ResponseEntity<ApiResponse<OperationPaymentResponseDto>> updateValidationReceipt(
            @PathVariable Long paymentId,
            @Valid @RequestBody UpdatePaymentStatusRequestDto request
    ) {
        OperationPaymentResponseDto response = paymentOperationService.updateValidationReceipt(paymentId, request);

        return ResponseEntity.ok(
                new ApiResponse<>(true, "Comprobante de validación actualizado exitosamente", response, null)
        );
    }

    @PatchMapping("/payments/{paymentId}/in-progress")
    @PreAuthorize("@rolePermissionPolicy.allows(authentication, 'PAYMENT_VALIDATE')")
    public ResponseEntity<ApiResponse<OperationPaymentResponseDto>> markPaymentInProgress(
            @PathVariable Long paymentId,
            @Valid @RequestBody(required = false) UpdatePaymentStatusRequestDto request
    ) {
        OperationPaymentResponseDto response = paymentOperationService.markPaymentInProgress(paymentId, request);

        return ResponseEntity.ok(
                new ApiResponse<>(true, "Comprobante marcado en proceso exitosamente", response, null)
        );
    }

    @PatchMapping("/payments/{paymentId}/release")
    @PreAuthorize("@rolePermissionPolicy.allows(authentication, 'PAYMENT_VALIDATE')")
    public ResponseEntity<ApiResponse<OperationPaymentResponseDto>> releasePaymentInProgress(
            @PathVariable Long paymentId
    ) {
        OperationPaymentResponseDto response = paymentOperationService.releasePaymentInProgress(paymentId);

        return ResponseEntity.ok(
                new ApiResponse<>(true, "Comprobante liberado exitosamente", response, null)
        );
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE', 'DIRECCION', 'SOCIO_COMERCIAL', 'JEFA_CAJAS', 'JEFA_CUENTAS', 'AUXILIAR_CUENTAS')")
    public ResponseEntity<ApiResponse<Page<PaymentOperationResponseDto>>> findAll(
            PaymentOperationFilterDto filter,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        Page<PaymentOperationResponseDto> response = paymentOperationService.findAll(filter, pageable);

        return ResponseEntity.ok(
                new ApiResponse<>(true, "Operaciones obtenidas exitosamente", response, null)
        );
    }

    @GetMapping("/by-commercial-partner/{socioComercialId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE', 'DIRECCION', 'JEFA_CAJAS')")
    public ResponseEntity<ApiResponse<Page<PaymentOperationResponseDto>>> findAllBySocioComercialId(
            @PathVariable Long socioComercialId,
            PaymentOperationFilterDto filter,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        Page<PaymentOperationResponseDto> response =
                paymentOperationService.findAllBySocioComercialId(socioComercialId, filter, pageable);

        return ResponseEntity.ok(
                new ApiResponse<>(true, "Operaciones del socio comercial obtenidas exitosamente", response, null)
        );
    }

    @GetMapping("/my-operations")
    @PreAuthorize("hasRole('SOCIO_COMERCIAL')")
    public ResponseEntity<ApiResponse<Page<PaymentOperationResponseDto>>> findMyOperations(
            PaymentOperationFilterDto filter,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        Page<PaymentOperationResponseDto> response = paymentOperationService.findMyOperations(filter, pageable);

        return ResponseEntity.ok(
                new ApiResponse<>(true, "Mis operaciones obtenidas exitosamente", response, null)
        );
    }

    @GetMapping("/frequent-clients")
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE', 'DIRECCION', 'SOCIO_COMERCIAL', 'JEFA_CAJAS', 'JEFA_CUENTAS', 'AUXILIAR_CUENTAS')")
    public ResponseEntity<ApiResponse<List<String>>> findFrequentClientNames() {
        List<String> response = paymentOperationService.findFrequentClientNames();

        return ResponseEntity.ok(
                new ApiResponse<>(true, "Clientes frecuentes obtenidos exitosamente", response, null)
        );
    }

    @GetMapping("/stalled")
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE', 'DIRECCION')")
    public ResponseEntity<ApiResponse<Page<PaymentOperationResponseDto>>> findStalledOperations(
            @RequestParam(defaultValue = "48") int thresholdHours,
            @PageableDefault(size = 20, sort = "updatedAt", direction = Sort.Direction.ASC) Pageable pageable
    ) {
        Page<PaymentOperationResponseDto> response =
                paymentOperationService.findStalledOperations(thresholdHours, pageable);

        return ResponseEntity.ok(
                new ApiResponse<>(true, "Operaciones detenidas obtenidas exitosamente", response, null)
        );
    }

    @GetMapping("/stalled/search")
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE', 'DIRECCION')")
    public ResponseEntity<ApiResponse<Page<PaymentOperationResponseDto>>> searchStalledOperations(
            @RequestParam(defaultValue = "") String search,
            @RequestParam(defaultValue = "48") int thresholdHours,
            @PageableDefault(size = 20, sort = "updatedAt", direction = Sort.Direction.ASC) Pageable pageable
    ) {
        Page<PaymentOperationResponseDto> response =
                paymentOperationService.searchStalledOperations(search, thresholdHours, pageable);

        return ResponseEntity.ok(
                new ApiResponse<>(true, "Operaciones detenidas obtenidas exitosamente", response, null)
        );
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE', 'DIRECCION', 'SOCIO_COMERCIAL', 'JEFA_CAJAS', 'JEFA_CUENTAS', 'AUXILIAR_CUENTAS')")
    public ResponseEntity<ApiResponse<PaymentOperationResponseDto>> findById(@PathVariable Long id) {
        PaymentOperationResponseDto response = paymentOperationService.findById(id);

        return ResponseEntity.ok(
                new ApiResponse<>(true, "Operación obtenida exitosamente", response, null)
        );
    }

    @PatchMapping("/{id}/activate")
    @PreAuthorize("@rolePermissionPolicy.allows(authentication, 'OPERATION_STATUS')")
    public ResponseEntity<ApiResponse<PaymentOperationResponseDto>> activate(@PathVariable Long id) {
        PaymentOperationResponseDto response = paymentOperationService.activate(id);

        return ResponseEntity.ok(
                new ApiResponse<>(true, "Operación activada exitosamente", response, null)
        );
    }

    @PatchMapping("/{id}/deactivate")
    @PreAuthorize("@rolePermissionPolicy.allows(authentication, 'OPERATION_STATUS')")
    public ResponseEntity<ApiResponse<PaymentOperationResponseDto>> deactivate(@PathVariable Long id) {
        PaymentOperationResponseDto response = paymentOperationService.deactivate(id);

        return ResponseEntity.ok(
                new ApiResponse<>(true, "Operación desactivada exitosamente", response, null)
        );
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@rolePermissionPolicy.allows(authentication, 'OPERATION_DELETE')")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        paymentOperationService.delete(id);

        return ResponseEntity.ok(
                new ApiResponse<>(true, "Operación eliminada permanentemente", null, null)
        );
    }

    @PatchMapping("/{operationId}/invoice")
    @PreAuthorize("@rolePermissionPolicy.allows(authentication, 'OPERATION_INVOICE')")
    public ResponseEntity<ApiResponse<PaymentOperationResponseDto>> markAsInvoiced(
            @PathVariable Long operationId
    ) {
        PaymentOperationResponseDto response = paymentOperationService.markAsInvoiced(operationId);

        return ResponseEntity.ok(
                new ApiResponse<>(true, "Operación marcada como facturada exitosamente", response, null)
        );
    }
}
