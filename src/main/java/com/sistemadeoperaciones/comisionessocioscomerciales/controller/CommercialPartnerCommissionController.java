package com.sistemadeoperaciones.comisionessocioscomerciales.controller;

import com.sistemadeoperaciones.comisionessocioscomerciales.dto.request.PayCommissionBatchRequestDto;
import com.sistemadeoperaciones.comisionessocioscomerciales.dto.request.PayCommissionRequestDto;
import com.sistemadeoperaciones.comisionessocioscomerciales.dto.response.*;
import com.sistemadeoperaciones.comisionessocioscomerciales.enums.CommissionBeneficiaryType;
import com.sistemadeoperaciones.comisionessocioscomerciales.service.CommercialPartnerCommissionService;
import com.sistemadeoperaciones.shared.dto.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/commercial-partner-commissions")
public class CommercialPartnerCommissionController {

    private final CommercialPartnerCommissionService commissionService;

    public CommercialPartnerCommissionController(
            CommercialPartnerCommissionService commissionService
    ) {
        this.commissionService = commissionService;
    }

    @PostMapping("/generate")
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE')")
    public ResponseEntity<ApiResponse<Void>> generatePendingCommissions() {

        commissionService.generatePendingCommissions();

        return ResponseEntity.ok(
                new ApiResponse<>(
                        true,
                        "Comisiones generadas exitosamente",
                        null,
                        null
                )
        );
    }

    @GetMapping("/summary")
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE', 'AUXILIAR_CUENTAS')")
    public ResponseEntity<ApiResponse<CommissionSummaryResponseDto>>
    getSummary(
            @RequestParam LocalDate startDate,
            @RequestParam LocalDate endDate
    ) {

        CommissionSummaryResponseDto response =
                commissionService.getSummary(
                        startDate,
                        endDate
                );

        return ResponseEntity.ok(
                new ApiResponse<>(
                        true,
                        "Resumen obtenido exitosamente",
                        response,
                        null
                )
        );
    }

    @GetMapping("/beneficiaries-summary")
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE', 'AUXILIAR_CUENTAS')")
    public ResponseEntity<ApiResponse<CommissionPartnerSummaryListResponseDto>>
    getSummaryByBeneficiary(
            @RequestParam LocalDate startDate,
            @RequestParam LocalDate endDate
    ) {

        CommissionPartnerSummaryListResponseDto response =
                commissionService.getSummaryByBeneficiary(
                        startDate,
                        endDate
                );

        return ResponseEntity.ok(
                new ApiResponse<>(
                        true,
                        "Resumen por beneficiario obtenido exitosamente",
                        response,
                        null
                )
        );
    }

    @GetMapping("/pending")
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE', 'AUXILIAR_CUENTAS')")
    public ResponseEntity<ApiResponse<CommissionSummaryResponseDto>>
    getPendingCommissions(
            @RequestParam LocalDate startDate,
            @RequestParam LocalDate endDate
    ) {

        CommissionSummaryResponseDto response =
                commissionService.getPendingCommissions(
                        startDate,
                        endDate
                );

        return ResponseEntity.ok(
                new ApiResponse<>(
                        true,
                        "Comisiones pendientes obtenidas exitosamente",
                        response,
                        null
                )
        );
    }

    @GetMapping("/paid")
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE', 'AUXILIAR_CUENTAS')")
    public ResponseEntity<ApiResponse<CommissionSummaryResponseDto>>
    getPaidCommissions(
            @RequestParam LocalDate startDate,
            @RequestParam LocalDate endDate
    ) {

        CommissionSummaryResponseDto response =
                commissionService.getPaidCommissions(
                        startDate,
                        endDate
                );

        return ResponseEntity.ok(
                new ApiResponse<>(
                        true,
                        "Comisiones pagadas obtenidas exitosamente",
                        response,
                        null
                )
        );
    }

    @GetMapping("/{commissionId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE', 'AUXILIAR_CUENTAS')")
    public ResponseEntity<ApiResponse<CommercialPartnerCommissionResponseDto>>
    findById(
            @PathVariable Long commissionId
    ) {

        CommercialPartnerCommissionResponseDto response =
                commissionService.findById(
                        commissionId
                );

        return ResponseEntity.ok(
                new ApiResponse<>(
                        true,
                        "Comisión obtenida exitosamente",
                        response,
                        null
                )
        );
    }

    @PatchMapping("/{commissionId}/pay")
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE', 'AUXILIAR_CUENTAS')")
    public ResponseEntity<ApiResponse<CommercialPartnerCommissionResponseDto>>
    markAsPaid(
            @PathVariable Long commissionId,
            @Valid @RequestBody PayCommissionRequestDto request
    ) {

        CommercialPartnerCommissionResponseDto response =
                commissionService.markAsPaid(
                        commissionId,
                        request
                );

        return ResponseEntity.ok(
                new ApiResponse<>(
                        true,
                        "Comisión pagada exitosamente",
                        response,
                        null
                )
        );
    }

    @PostMapping("/pay-batch")
    @PreAuthorize(
            "hasAnyRole('ADMIN', 'GERENTE', 'AUXILIAR_CUENTAS')"
    )
    public ResponseEntity<ApiResponse<Void>>
    markBatchAsPaid(
            @Valid
            @RequestBody
            PayCommissionBatchRequestDto request
    ) {

        commissionService.markBatchAsPaid(
                request
        );

        return ResponseEntity.ok(
                new ApiResponse<>(
                        true,
                        "Comisiones pagadas exitosamente",
                        null,
                        null
                )
        );
    }

    @PostMapping("/operations/{operationId}/regenerate")
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE')")
    public ResponseEntity<ApiResponse<Void>>
    regenerateOperationCommissions(
            @PathVariable Long operationId
    ) {

        commissionService.regenerateOperationCommissions(
                operationId
        );

        return ResponseEntity.ok(
                new ApiResponse<>(
                        true,
                        "Comisiones regeneradas exitosamente",
                        null,
                        null
                )
        );
    }

    @GetMapping("/operations/{operationId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE', 'AUXILIAR_CUENTAS')")
    public ResponseEntity<ApiResponse<CommissionOperationDetailResponseDto>>
    getOperationDetail(
            @PathVariable Long operationId
    ) {

        CommissionOperationDetailResponseDto response =
                commissionService.getOperationDetail(
                        operationId
                );

        return ResponseEntity.ok(
                new ApiResponse<>(
                        true,
                        "Detalle de operación obtenido exitosamente",
                        response,
                        null
                )
        );
    }

    @GetMapping("/operations/{operationId}/beneficiaries")
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE', 'AUXILIAR_CUENTAS')")
    public ResponseEntity<ApiResponse<List<CommissionBeneficiaryResponseDto>>>
    getOperationBeneficiaries(
            @PathVariable Long operationId
    ) {

        List<CommissionBeneficiaryResponseDto> response =
                commissionService.getOperationBeneficiaries(
                        operationId
                );

        return ResponseEntity.ok(
                new ApiResponse<>(
                        true,
                        "Beneficiarios obtenidos exitosamente",
                        response,
                        null
                )
        );
    }

    @GetMapping("/beneficiaries/{beneficiaryId}/detail")
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE', 'AUXILIAR_CUENTAS')")
    public ResponseEntity<ApiResponse<BeneficiaryCommissionDetailResponseDto>>
    getBeneficiaryCommissionDetail(
            @PathVariable Long beneficiaryId,
            @RequestParam CommissionBeneficiaryType beneficiaryType,
            @RequestParam LocalDate startDate,
            @RequestParam LocalDate endDate
    ) {

        BeneficiaryCommissionDetailResponseDto response =
                commissionService.getBeneficiaryCommissionDetail(
                        beneficiaryId,
                        beneficiaryType,
                        startDate,
                        endDate
                );

        return ResponseEntity.ok(
                new ApiResponse<>(
                        true,
                        "Detalle del beneficiario obtenido exitosamente",
                        response,
                        null
                )
        );
    }

    @GetMapping("/my-weekly-commissions")
    @PreAuthorize("hasAnyRole('ADMIN','SOCIO_COMERCIAL')")
    public ResponseEntity<ApiResponse<MyWeeklyCommissionsResponseDto>>
    getMyWeeklyCommissions(
            @RequestParam LocalDate startDate,
            @RequestParam LocalDate endDate
    ) {

        MyWeeklyCommissionsResponseDto response =
                commissionService.getMyWeeklyCommissions(
                        startDate,
                        endDate
                );

        return ResponseEntity.ok(
                new ApiResponse<>(
                        true,
                        "Comisiones semanales obtenidas exitosamente",
                        response,
                        null
                )
        );
    }
}