package com.sistemadeoperaciones.comisionessocioscomerciales.dto.response;

import java.math.BigDecimal;
import java.util.List;

public class CommissionPartnerSummaryListResponseDto {

    private BigDecimal totalComisiones;

    private BigDecimal totalPagadas;

    private BigDecimal totalPendientes;

    private Integer totalBeneficiarios;

    private List<CommissionPartnerSummaryResponseDto> socios;

    public CommissionPartnerSummaryListResponseDto() {
    }

    public CommissionPartnerSummaryListResponseDto(BigDecimal totalComisiones, BigDecimal totalPagadas, BigDecimal totalPendientes, Integer totalBeneficiarios, List<CommissionPartnerSummaryResponseDto> socios) {
        this.totalComisiones = totalComisiones;
        this.totalPagadas = totalPagadas;
        this.totalPendientes = totalPendientes;
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