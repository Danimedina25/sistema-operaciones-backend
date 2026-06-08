package com.sistemadeoperaciones.comisionessocioscomerciales.dto.response;

import com.sistemadeoperaciones.pagos.enums.CommissionStatus;

import java.math.BigDecimal;

public class CommissionBeneficiaryResponseDto {

    private Long commissionId;

    private Long operationId;

    private Integer nivel;

    private String nombre;

    private String banco;

    private String cuentaBancaria;

    private String titularCuenta;

    private BigDecimal commissionAmount;

    private CommissionStatus status;

    private String paymentProofUrl;

    private BigDecimal commissionPercentage;

    public CommissionBeneficiaryResponseDto() {
    }

    public CommissionBeneficiaryResponseDto(Long commissionId, Long operationId, Integer nivel, String nombre, String banco, String cuentaBancaria, String titularCuenta, BigDecimal commissionAmount, CommissionStatus status, String paymentProofUrl, BigDecimal commissionPercentage) {
        this.commissionId = commissionId;
        this.operationId = operationId;
        this.nivel = nivel;
        this.nombre = nombre;
        this.banco = banco;
        this.cuentaBancaria = cuentaBancaria;
        this.titularCuenta = titularCuenta;
        this.commissionAmount = commissionAmount;
        this.status = status;
        this.paymentProofUrl = paymentProofUrl;
        this.commissionPercentage = commissionPercentage;
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

    public Integer getNivel() {
        return nivel;
    }

    public void setNivel(Integer nivel) {
        this.nivel = nivel;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getBanco() {
        return banco;
    }

    public void setBanco(String banco) {
        this.banco = banco;
    }

    public String getCuentaBancaria() {
        return cuentaBancaria;
    }

    public void setCuentaBancaria(String cuentaBancaria) {
        this.cuentaBancaria = cuentaBancaria;
    }

    public String getTitularCuenta() {
        return titularCuenta;
    }

    public void setTitularCuenta(String titularCuenta) {
        this.titularCuenta = titularCuenta;
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

    public BigDecimal getCommissionPercentage() {
        return commissionPercentage;
    }

    public void setCommissionPercentage(BigDecimal commissionPercentage) {
        this.commissionPercentage = commissionPercentage;
    }
}