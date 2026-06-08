package com.sistemadeoperaciones.comisionessocioscomerciales.dto.response;

import com.sistemadeoperaciones.pagos.enums.CommissionStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class CommercialPartnerCommissionResponseDto {

    private Long id;

    private Long operationId;

    private Long userId;
    private Long commercialPartnerId;

    private String nombreBeneficiario;

    private Integer nivel;

    private BigDecimal commissionAmount;

    private CommissionStatus status;

    private String paymentProofUrl;

    private LocalDateTime generatedAt;
    private LocalDateTime paidAt;

    public CommercialPartnerCommissionResponseDto() {
    }

    public CommercialPartnerCommissionResponseDto(
            Long id,
            Long operationId,
            Long userId,
            Long commercialPartnerId,
            String nombreBeneficiario,
            Integer nivel,
            BigDecimal commissionAmount,
            CommissionStatus status,
            String paymentProofUrl,
            LocalDateTime generatedAt,
            LocalDateTime paidAt
    ) {
        this.id = id;
        this.operationId = operationId;
        this.userId = userId;
        this.commercialPartnerId = commercialPartnerId;
        this.nombreBeneficiario = nombreBeneficiario;
        this.nivel = nivel;
        this.commissionAmount = commissionAmount;
        this.status = status;
        this.paymentProofUrl = paymentProofUrl;
        this.generatedAt = generatedAt;
        this.paidAt = paidAt;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getOperationId() {
        return operationId;
    }

    public void setOperationId(Long operationId) {
        this.operationId = operationId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getCommercialPartnerId() {
        return commercialPartnerId;
    }

    public void setCommercialPartnerId(Long commercialPartnerId) {
        this.commercialPartnerId = commercialPartnerId;
    }

    public String getNombreBeneficiario() {
        return nombreBeneficiario;
    }

    public void setNombreBeneficiario(String nombreBeneficiario) {
        this.nombreBeneficiario = nombreBeneficiario;
    }

    public Integer getNivel() {
        return nivel;
    }

    public void setNivel(Integer nivel) {
        this.nivel = nivel;
    }

    public BigDecimal getCommissionAmount() {
        return commissionAmount;
    }

    public void setCommissionAmount(BigDecimal commissionAmount) {
        this.commissionAmount = commissionAmount;
    }

    public CommissionStatus getStatus() {
        return status;
    }

    public void setStatus(CommissionStatus status) {
        this.status = status;
    }

    public String getPaymentProofUrl() {
        return paymentProofUrl;
    }

    public void setPaymentProofUrl(String paymentProofUrl) {
        this.paymentProofUrl = paymentProofUrl;
    }

    public LocalDateTime getGeneratedAt() {
        return generatedAt;
    }

    public void setGeneratedAt(LocalDateTime generatedAt) {
        this.generatedAt = generatedAt;
    }

    public LocalDateTime getPaidAt() {
        return paidAt;
    }

    public void setPaidAt(LocalDateTime paidAt) {
        this.paidAt = paidAt;
    }
}