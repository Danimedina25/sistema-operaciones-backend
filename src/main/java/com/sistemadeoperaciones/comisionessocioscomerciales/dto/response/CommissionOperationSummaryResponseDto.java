package com.sistemadeoperaciones.comisionessocioscomerciales.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;

public class CommissionOperationSummaryResponseDto {

    private Long operationId;

    private String cliente;

    private Integer nivelesRedComercial;

    private Integer totalBeneficiarios;

    private BigDecimal montoOperacion;

    private BigDecimal totalComisiones;

    private Boolean pagadaCompletamente;

    private Boolean pagadaParcialmente;

    private LocalDate fechaOperacion;

    public CommissionOperationSummaryResponseDto() {
    }

    public CommissionOperationSummaryResponseDto(Long operationId, String cliente, Integer nivelesRedComercial, Integer totalBeneficiarios, BigDecimal montoOperacion, BigDecimal totalComisiones, Boolean pagadaCompletamente, Boolean pagadaParcialmente, LocalDate fechaOperacion) {
        this.operationId = operationId;
        this.cliente = cliente;
        this.nivelesRedComercial = nivelesRedComercial;
        this.totalBeneficiarios = totalBeneficiarios;
        this.montoOperacion = montoOperacion;
        this.totalComisiones = totalComisiones;
        this.pagadaCompletamente = pagadaCompletamente;
        this.pagadaParcialmente = pagadaParcialmente;
        this.fechaOperacion = fechaOperacion;
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

    public Integer getNivelesRedComercial() {
        return nivelesRedComercial;
    }

    public void setNivelesRedComercial(Integer nivelesRedComercial) {
        this.nivelesRedComercial = nivelesRedComercial;
    }

    public Integer getTotalBeneficiarios() {
        return totalBeneficiarios;
    }

    public void setTotalBeneficiarios(Integer totalBeneficiarios) {
        this.totalBeneficiarios = totalBeneficiarios;
    }

    public BigDecimal getMontoOperacion() {
        return montoOperacion;
    }

    public void setMontoOperacion(BigDecimal montoOperacion) {
        this.montoOperacion = montoOperacion;
    }

    public BigDecimal getTotalComisiones() {
        return totalComisiones;
    }

    public void setTotalComisiones(BigDecimal totalComisiones) {
        this.totalComisiones = totalComisiones;
    }

    public Boolean getPagadaCompletamente() {
        return pagadaCompletamente;
    }

    public void setPagadaCompletamente(Boolean pagadaCompletamente) {
        this.pagadaCompletamente = pagadaCompletamente;
    }

    public LocalDate getFechaOperacion() {
        return fechaOperacion;
    }

    public void setFechaOperacion(LocalDate fechaOperacion) {
        this.fechaOperacion = fechaOperacion;
    }


    public Boolean getPagadaParcialmente() {
        return pagadaParcialmente;
    }

    public void setPagadaParcialmente(Boolean pagadaParcialmente) {
        this.pagadaParcialmente = pagadaParcialmente;
    }
}