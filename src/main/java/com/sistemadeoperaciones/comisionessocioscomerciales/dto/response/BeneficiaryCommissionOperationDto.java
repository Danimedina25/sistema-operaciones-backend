package com.sistemadeoperaciones.comisionessocioscomerciales.dto.response;

import com.sistemadeoperaciones.pagos.enums.CommissionStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class BeneficiaryCommissionOperationDto {

    private Long commissionId;

    private Long operationId;

    private String clienteNombre;

    private LocalDateTime operationDate;

    private Integer nivel;

    private BigDecimal operationAmount;

    private BigDecimal commissionPercentage;

    private BigDecimal commissionAmount;

    private CommissionStatus commissionStatus;

    public BeneficiaryCommissionOperationDto(
            Long commissionId,
            Long operationId,
            String clienteNombre,
            LocalDateTime operationDate,
            Integer nivel,
            BigDecimal operationAmount,
            BigDecimal commissionPercentage,
            BigDecimal commissionAmount,
            CommissionStatus commissionStatus
    ) {
        this.commissionId = commissionId;
        this.operationId = operationId;
        this.clienteNombre = clienteNombre;
        this.operationDate = operationDate;
        this.nivel = nivel;
        this.operationAmount = operationAmount;
        this.commissionPercentage = commissionPercentage;
        this.commissionAmount = commissionAmount;
        this.commissionStatus = commissionStatus;
    }

    public BeneficiaryCommissionOperationDto() {
    }

    public Long getCommissionId() {
        return commissionId;
    }

    public void setCommissionId(Long commissionId) {
        this.commissionId = commissionId;
    }

    public Long getOperationId() {
        return operationId;
    }

    public void setOperationId(Long operationId) {
        this.operationId = operationId;
    }

    public String getClienteNombre() {
        return clienteNombre;
    }

    public void setClienteNombre(String clienteNombre) {
        this.clienteNombre = clienteNombre;
    }

    public LocalDateTime getOperationDate() {
        return operationDate;
    }

    public void setOperationDate(LocalDateTime operationDate) {
        this.operationDate = operationDate;
    }

    public Integer getNivel() {
        return nivel;
    }

    public void setNivel(Integer nivel) {
        this.nivel = nivel;
    }

    public BigDecimal getOperationAmount() {
        return operationAmount;
    }

    public void setOperationAmount(BigDecimal operationAmount) {
        this.operationAmount = operationAmount;
    }

    public BigDecimal getCommissionPercentage() {
        return commissionPercentage;
    }

    public void setCommissionPercentage(BigDecimal commissionPercentage) {
        this.commissionPercentage = commissionPercentage;
    }

    public BigDecimal getCommissionAmount() {
        return commissionAmount;
    }

    public void setCommissionAmount(BigDecimal commissionAmount) {
        this.commissionAmount = commissionAmount;
    }

    public CommissionStatus getCommissionStatus() {
        return commissionStatus;
    }

    public void setCommissionStatus(CommissionStatus commissionStatus) {
        this.commissionStatus = commissionStatus;
    }
}