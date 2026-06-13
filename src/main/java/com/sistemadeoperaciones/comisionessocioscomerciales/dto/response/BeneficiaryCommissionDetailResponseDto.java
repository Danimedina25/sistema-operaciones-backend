package com.sistemadeoperaciones.comisionessocioscomerciales.dto.response;

import com.sistemadeoperaciones.comisionessocioscomerciales.enums.CommissionBeneficiaryType;

import java.math.BigDecimal;
import java.util.List;

public class BeneficiaryCommissionDetailResponseDto {

    private Long beneficiaryId;

    private String beneficiaryName;

    private CommissionBeneficiaryType beneficiaryType;

    private Integer totalOperations;

    private BigDecimal totalCommission;

    private List<BeneficiaryCommissionOperationDto> operations;

    public BeneficiaryCommissionDetailResponseDto(
            Long beneficiaryId,
            String beneficiaryName,
            CommissionBeneficiaryType beneficiaryType,
            Integer totalOperations,
            BigDecimal totalCommission,
            List<BeneficiaryCommissionOperationDto> operations
    ) {
        this.beneficiaryId = beneficiaryId;
        this.beneficiaryName = beneficiaryName;
        this.beneficiaryType = beneficiaryType;
        this.totalOperations = totalOperations;
        this.totalCommission = totalCommission;
        this.operations = operations;
    }

    public BeneficiaryCommissionDetailResponseDto() {
    }

    public Long getBeneficiaryId() {
        return beneficiaryId;
    }

    public void setBeneficiaryId(Long beneficiaryId) {
        this.beneficiaryId = beneficiaryId;
    }

    public String getBeneficiaryName() {
        return beneficiaryName;
    }

    public void setBeneficiaryName(String beneficiaryName) {
        this.beneficiaryName = beneficiaryName;
    }

    public CommissionBeneficiaryType getBeneficiaryType() {
        return beneficiaryType;
    }

    public void setBeneficiaryType(CommissionBeneficiaryType beneficiaryType) {
        this.beneficiaryType = beneficiaryType;
    }

    public Integer getTotalOperations() {
        return totalOperations;
    }

    public void setTotalOperations(Integer totalOperations) {
        this.totalOperations = totalOperations;
    }

    public BigDecimal getTotalCommission() {
        return totalCommission;
    }

    public void setTotalCommission(BigDecimal totalCommission) {
        this.totalCommission = totalCommission;
    }

    public List<BeneficiaryCommissionOperationDto> getOperations() {
        return operations;
    }

    public void setOperations(List<BeneficiaryCommissionOperationDto> operations) {
        this.operations = operations;
    }
}