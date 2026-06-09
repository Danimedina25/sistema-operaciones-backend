package com.sistemadeoperaciones.comisionessocioscomerciales.dto.response;

import com.sistemadeoperaciones.comisionessocioscomerciales.enums.CommissionBeneficiaryType;

import java.math.BigDecimal;
import java.util.List;

public class CommissionPartnerSummaryResponseDto {

    private Long beneficiaryId;

    private CommissionBeneficiaryType beneficiaryType;

    private String nombre;

    private String banco;

    private String cuentaBancaria;

    private String titularCuenta;

    private Integer totalOperaciones;

    private BigDecimal totalComisiones;

    private BigDecimal totalPendientes;

    private BigDecimal totalPagadas;

    private Integer totalComisionesPendientes;

    private List<Long> commissionIdsToPay;

    private String paymentProofUrl;

    public CommissionPartnerSummaryResponseDto() {
    }

    public CommissionPartnerSummaryResponseDto(Long beneficiaryId, CommissionBeneficiaryType beneficiaryType, String nombre, String banco, String cuentaBancaria, String titularCuenta, Integer totalOperaciones, BigDecimal totalComisiones, BigDecimal totalPendientes, BigDecimal totalPagadas, Integer totalComisionesPendientes, List<Long> commissionIdsToPay, String paymentProofUrl) {
        this.beneficiaryId = beneficiaryId;
        this.beneficiaryType = beneficiaryType;
        this.nombre = nombre;
        this.banco = banco;
        this.cuentaBancaria = cuentaBancaria;
        this.titularCuenta = titularCuenta;
        this.totalOperaciones = totalOperaciones;
        this.totalComisiones = totalComisiones;
        this.totalPendientes = totalPendientes;
        this.totalPagadas = totalPagadas;
        this.totalComisionesPendientes = totalComisionesPendientes;
        this.commissionIdsToPay = commissionIdsToPay;
        this.paymentProofUrl = paymentProofUrl;
    }

    public Long getBeneficiaryId() {
        return beneficiaryId;
    }

    public void setBeneficiaryId(Long beneficiaryId) {
        this.beneficiaryId = beneficiaryId;
    }

    public CommissionBeneficiaryType getBeneficiaryType() {
        return beneficiaryType;
    }

    public void setBeneficiaryType(CommissionBeneficiaryType beneficiaryType) {
        this.beneficiaryType = beneficiaryType;
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

    public Integer getTotalOperaciones() {
        return totalOperaciones;
    }

    public void setTotalOperaciones(Integer totalOperaciones) {
        this.totalOperaciones = totalOperaciones;
    }

    public BigDecimal getTotalComisiones() {
        return totalComisiones;
    }

    public void setTotalComisiones(BigDecimal totalComisiones) {
        this.totalComisiones = totalComisiones;
    }

    public BigDecimal getTotalPendientes() {
        return totalPendientes;
    }

    public void setTotalPendientes(BigDecimal totalPendientes) {
        this.totalPendientes = totalPendientes;
    }

    public BigDecimal getTotalPagadas() {
        return totalPagadas;
    }

    public void setTotalPagadas(BigDecimal totalPagadas) {
        this.totalPagadas = totalPagadas;
    }

    public Integer getTotalComisionesPendientes() {
        return totalComisionesPendientes;
    }

    public void setTotalComisionesPendientes(Integer totalComisionesPendientes) {
        this.totalComisionesPendientes = totalComisionesPendientes;
    }

    public List<Long> getCommissionIdsToPay() {
        return commissionIdsToPay;
    }

    public void setCommissionIdsToPay(List<Long> commissionIdsToPay) {
        this.commissionIdsToPay = commissionIdsToPay;
    }

    public String getPaymentProofUrl() {
        return paymentProofUrl;
    }

    public void setPaymentProofUrl(String paymentProofUrl) {
        this.paymentProofUrl = paymentProofUrl;
    }
}