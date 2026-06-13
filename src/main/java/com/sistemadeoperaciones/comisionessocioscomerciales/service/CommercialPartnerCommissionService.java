package com.sistemadeoperaciones.comisionessocioscomerciales.service;


import com.sistemadeoperaciones.comisionessocioscomerciales.dto.request.PayCommissionBatchRequestDto;
import com.sistemadeoperaciones.comisionessocioscomerciales.dto.request.PayCommissionRequestDto;
import com.sistemadeoperaciones.comisionessocioscomerciales.dto.response.*;
import com.sistemadeoperaciones.comisionessocioscomerciales.enums.CommissionBeneficiaryType;

import java.time.LocalDate;
import java.util.List;

public interface CommercialPartnerCommissionService {

    /**
     * Genera todas las comisiones faltantes
     * para operaciones validadas.
     */
    void generatePendingCommissions();

    /**
     * Lista de operaciones con comisiones
     * generadas para la semana.
     */
    CommissionOperationDetailResponseDto getOperationDetail(
            Long operationId
    );

    /**
     * Obtener una comisión específica.
     */
    CommercialPartnerCommissionResponseDto findById(
            Long commissionId
    );

    /**
     * Marcar una comisión como pagada.
     */
    CommercialPartnerCommissionResponseDto markAsPaid(
            Long commissionId,
            PayCommissionRequestDto request
    );

    void markBatchAsPaid(
            PayCommissionBatchRequestDto request
    );

    /**
     * Obtener comisiones generadas
     * para una semana.
     */
    CommissionSummaryResponseDto getSummary(
            LocalDate startDate,
            LocalDate endDate
    );

    MyWeeklyCommissionsResponseDto getMyWeeklyCommissions(
            LocalDate startDate,
            LocalDate endDate
    );

    CommissionPartnerSummaryListResponseDto
    getSummaryByBeneficiary(
            LocalDate startDate,
            LocalDate endDate
    );

    /**
     * Obtener pendientes.
     */
    CommissionSummaryResponseDto getPendingCommissions(LocalDate startDate,
                                                       LocalDate endDate);

    /**
     * Obtener pagadas.
     */
    CommissionSummaryResponseDto getPaidCommissions(LocalDate startDate,
                                                    LocalDate endDate);

    /**
     * Regenerar comisiones
     * para una operación.
     */
    void regenerateOperationCommissions(
            Long operationId
    );

    List<CommissionOperationSummaryResponseDto>
    getWeeklyOperations(LocalDate weekDate);

    List<CommissionBeneficiaryResponseDto>
    getOperationBeneficiaries(Long operationId);

    BeneficiaryCommissionDetailResponseDto getBeneficiaryCommissionDetail(
            Long beneficiaryId,
            CommissionBeneficiaryType beneficiaryType,
            LocalDate startDate,
            LocalDate endDate
    );
}