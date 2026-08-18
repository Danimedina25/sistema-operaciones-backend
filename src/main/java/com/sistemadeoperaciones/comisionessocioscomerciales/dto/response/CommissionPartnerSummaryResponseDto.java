package com.sistemadeoperaciones.comisionessocioscomerciales.dto.response;

import com.sistemadeoperaciones.comisionessocioscomerciales.enums.CommissionBeneficiaryType;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public class CommissionPartnerSummaryResponseDto {

    private Long beneficiaryId;

    private CommissionBeneficiaryType beneficiaryType;

    private String nombre;

    private String banco;

    private String cuentaBancaria;

    private String titularCuenta;

    /**
     * Solo poblado cuando beneficiaryType == USER (User.telefono). Los
     * CommercialPartner (nivel 2/3, sin cuenta de usuario) no tienen
     * columna de teléfono en el sistema, así que queda null para ellos.
     */
    private String telefono;

    private Integer totalOperaciones;

    /**
     * Suma de `montoTotal` de las operaciones (distintas, sin duplicar)
     * que generaron comisión para este beneficiario en el periodo.
     */
    private BigDecimal montoOperado;

    private BigDecimal totalComisiones;

    private BigDecimal totalPendientes;

    private BigDecimal totalPagadas;

    private LocalDateTime fechaPagada;

    private Integer totalComisionesPendientes;

    private List<Long> commissionIdsToPay;

    private String paymentProofUrl;

    /**
     * Estatus autoritativo calculado en el servidor a partir de
     * commissionIdsToPay (PAGADA/PARCIAL/PENDIENTE), para que el frontend
     * no tenga que derivarlo por su cuenta a partir de totalPendientes
     * (que puede dar 0 con comisiones $0 aún GENERADA).
     */
    private String estatus;

    public CommissionPartnerSummaryResponseDto() {
    }

    public CommissionPartnerSummaryResponseDto(Long beneficiaryId, CommissionBeneficiaryType beneficiaryType, String nombre, String banco, String cuentaBancaria, String titularCuenta, String telefono, Integer totalOperaciones, BigDecimal montoOperado, BigDecimal totalComisiones, BigDecimal totalPendientes, BigDecimal totalPagadas, LocalDateTime fechaPagada, Integer totalComisionesPendientes, List<Long> commissionIdsToPay, String paymentProofUrl, String estatus) {
        this.beneficiaryId = beneficiaryId;
        this.beneficiaryType = beneficiaryType;
        this.nombre = nombre;
        this.banco = banco;
        this.cuentaBancaria = cuentaBancaria;
        this.titularCuenta = titularCuenta;
        this.telefono = telefono;
        this.totalOperaciones = totalOperaciones;
        this.montoOperado = montoOperado;
        this.totalComisiones = totalComisiones;
        this.totalPendientes = totalPendientes;
        this.totalPagadas = totalPagadas;
        this.fechaPagada = fechaPagada;
        this.totalComisionesPendientes = totalComisionesPendientes;
        this.commissionIdsToPay = commissionIdsToPay;
        this.paymentProofUrl = paymentProofUrl;
        this.estatus = estatus;
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

    public String getTelefono() {
        return telefono;
    }

    public void setTelefono(String telefono) {
        this.telefono = telefono;
    }

    public Integer getTotalOperaciones() {
        return totalOperaciones;
    }

    public void setTotalOperaciones(Integer totalOperaciones) {
        this.totalOperaciones = totalOperaciones;
    }

    public BigDecimal getMontoOperado() {
        return montoOperado;
    }

    public void setMontoOperado(BigDecimal montoOperado) {
        this.montoOperado = montoOperado;
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

    public LocalDateTime getFechaPagada() {
        return fechaPagada;
    }

    public void setFechaPagada(LocalDateTime fechaPagada) {
        this.fechaPagada = fechaPagada;
    }

    public String getEstatus() {
        return estatus;
    }

    public void setEstatus(String estatus) {
        this.estatus = estatus;
    }
}