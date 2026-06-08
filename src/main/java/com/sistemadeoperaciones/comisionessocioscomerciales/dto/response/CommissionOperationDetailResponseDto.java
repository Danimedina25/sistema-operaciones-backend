package com.sistemadeoperaciones.comisionessocioscomerciales.dto.response;

import java.math.BigDecimal;
import java.util.List;

public class CommissionOperationDetailResponseDto {

    private Long operationId;

    private String cliente;

    private BigDecimal montoOperacion;

    private BigDecimal porcentajeComisionIndividual;

    private BigDecimal porcentajeComisionTotalRed;

    private Integer nivelesRedComercial;

    private List<CommissionBeneficiaryResponseDto> beneficiarios;

    private BigDecimal totalCommissionAmount;

    public CommissionOperationDetailResponseDto() {
    }

    public CommissionOperationDetailResponseDto(Long operationId, String cliente, BigDecimal montoOperacion, BigDecimal porcentajeComisionIndividual, BigDecimal porcentajeComisionTotalRed, Integer nivelesRedComercial, List<CommissionBeneficiaryResponseDto> beneficiarios, BigDecimal totalCommissionAmount) {
        this.operationId = operationId;
        this.cliente = cliente;
        this.montoOperacion = montoOperacion;
        this.porcentajeComisionIndividual = porcentajeComisionIndividual;
        this.porcentajeComisionTotalRed = porcentajeComisionTotalRed;
        this.nivelesRedComercial = nivelesRedComercial;
        this.beneficiarios = beneficiarios;
        this.totalCommissionAmount = totalCommissionAmount;
    }

    public Long getOperationId() {
        return operationId;
    }

    public void setOperationId(Long operationId) {
        this.operationId = operationId;
    }

    public String getCliente() {
        return cliente;
    }

    public void setCliente(String cliente) {
        this.cliente = cliente;
    }

    public BigDecimal getMontoOperacion() {
        return montoOperacion;
    }

    public void setMontoOperacion(BigDecimal montoOperacion) {
        this.montoOperacion = montoOperacion;
    }

    public BigDecimal getPorcentajeComisionIndividual() {
        return porcentajeComisionIndividual;
    }

    public void setPorcentajeComisionIndividual(BigDecimal porcentajeComisionIndividual) {
        this.porcentajeComisionIndividual = porcentajeComisionIndividual;
    }

    public BigDecimal getPorcentajeComisionTotalRed() {
        return porcentajeComisionTotalRed;
    }

    public void setPorcentajeComisionTotalRed(BigDecimal porcentajeComisionTotalRed) {
        this.porcentajeComisionTotalRed = porcentajeComisionTotalRed;
    }

    public Integer getNivelesRedComercial() {
        return nivelesRedComercial;
    }

    public void setNivelesRedComercial(Integer nivelesRedComercial) {
        this.nivelesRedComercial = nivelesRedComercial;
    }

    public List<CommissionBeneficiaryResponseDto> getBeneficiarios() {
        return beneficiarios;
    }

    public void setBeneficiarios(List<CommissionBeneficiaryResponseDto> beneficiarios) {
        this.beneficiarios = beneficiarios;
    }

    public BigDecimal getTotalCommissionAmount() {
        return totalCommissionAmount;
    }

    public void setTotalCommissionAmount(BigDecimal totalCommissionAmount) {
        this.totalCommissionAmount = totalCommissionAmount;
    }
}