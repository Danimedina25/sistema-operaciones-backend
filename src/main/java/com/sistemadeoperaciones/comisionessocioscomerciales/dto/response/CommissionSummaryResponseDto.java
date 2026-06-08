package com.sistemadeoperaciones.comisionessocioscomerciales.dto.response;

import java.math.BigDecimal;
import java.util.List;

public class CommissionSummaryResponseDto {

    private BigDecimal totalComisiones;

    private BigDecimal totalPagadas;

    private BigDecimal totalPendientes;

    private Integer totalOperaciones;

    private Integer totalBeneficiarios;

    private List<CommissionOperationSummaryResponseDto> operaciones;

    public CommissionSummaryResponseDto() {
    }

    public CommissionSummaryResponseDto(
            BigDecimal totalComisiones,
            BigDecimal totalPagadas,
            BigDecimal totalPendientes,
            Integer totalOperaciones,
            Integer totalBeneficiarios,
            List<CommissionOperationSummaryResponseDto> operaciones
    ) {
        this.totalComisiones = totalComisiones;
        this.totalPagadas = totalPagadas;
        this.totalPendientes = totalPendientes;
        this.totalOperaciones = totalOperaciones;
        this.totalBeneficiarios = totalBeneficiarios;
        this.operaciones = operaciones;
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

    public Integer getTotalOperaciones() {
        return totalOperaciones;
    }

    public void setTotalOperaciones(Integer totalOperaciones) {
        this.totalOperaciones = totalOperaciones;
    }

    public Integer getTotalBeneficiarios() {
        return totalBeneficiarios;
    }

    public void setTotalBeneficiarios(Integer totalBeneficiarios) {
        this.totalBeneficiarios = totalBeneficiarios;
    }

    public List<CommissionOperationSummaryResponseDto> getOperaciones() {
        return operaciones;
    }

    public void setOperaciones(List<CommissionOperationSummaryResponseDto> operaciones) {
        this.operaciones = operaciones;
    }
}