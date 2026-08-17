package com.sistemadeoperaciones.comisionessocioscomerciales.dto.response;

import java.math.BigDecimal;
import java.util.List;

public class CommissionPartnerSummaryListResponseDto {

    private BigDecimal totalComisiones;

    private BigDecimal totalPagadas;

    private BigDecimal totalPendientes;

    /**
     * Suma de `montoOperado` de todos los socios — volumen operado en el
     * periodo que ya generó comisión (operaciones validadas), no todo lo
     * creado.
     */
    private BigDecimal totalMontoOperado;

    private Integer totalBeneficiarios;

    private List<CommissionPartnerSummaryResponseDto> socios;

    public CommissionPartnerSummaryListResponseDto() {
    }

    public CommissionPartnerSummaryListResponseDto(BigDecimal totalComisiones, BigDecimal totalPagadas, BigDecimal totalPendientes, BigDecimal totalMontoOperado, Integer totalBeneficiarios, List<CommissionPartnerSummaryResponseDto> socios) {
        this.totalComisiones = totalComisiones;
        this.totalPagadas = totalPagadas;
        this.totalPendientes = totalPendientes;
        this.totalMontoOperado = totalMontoOperado;
        this.totalBeneficiarios = totalBeneficiarios;
        this.socios = socios;
    }

    public BigDecimal getTotalComisiones() {
        return totalComisiones;
    }

    public void setTotalComisiones(BigDecimal totalComisiones) {
        this.totalComisiones = totalComisiones;
    }

    public BigDecimal getTotalPagadas() {
        return totalPagadas;
    }

    public void setTotalPagadas(BigDecimal totalPagadas) {
        this.totalPagadas = totalPagadas;
    }

    public BigDecimal getTotalPendientes() {
        return totalPendientes;
    }

    public void setTotalPendientes(BigDecimal totalPendientes) {
        this.totalPendientes = totalPendientes;
    }

    public BigDecimal getTotalMontoOperado() {
        return totalMontoOperado;
    }

    public void setTotalMontoOperado(BigDecimal totalMontoOperado) {
        this.totalMontoOperado = totalMontoOperado;
    }

    public Integer getTotalBeneficiarios() {
        return totalBeneficiarios;
    }

    public void setTotalBeneficiarios(Integer totalBeneficiarios) {
        this.totalBeneficiarios = totalBeneficiarios;
    }

    public List<CommissionPartnerSummaryResponseDto> getSocios() {
        return socios;
    }

    public void setSocios(List<CommissionPartnerSummaryResponseDto> socios) {
        this.socios = socios;
    }
}