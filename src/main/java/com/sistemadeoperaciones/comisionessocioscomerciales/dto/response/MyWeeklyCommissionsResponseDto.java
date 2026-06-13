package com.sistemadeoperaciones.comisionessocioscomerciales.dto.response;

import java.math.BigDecimal;
import java.util.List;

public class MyWeeklyCommissionsResponseDto {

    private BigDecimal totalGanado;

    private BigDecimal totalGanadoRed;

    private Integer totalOperaciones;

    private List<MyWeeklyCommissionOperationDto> operaciones;

    public MyWeeklyCommissionsResponseDto(BigDecimal totalGanado, BigDecimal totalGanadoRed, Integer totalOperaciones, List<MyWeeklyCommissionOperationDto> operaciones) {
        this.totalGanado = totalGanado;
        this.totalGanadoRed = totalGanadoRed;
        this.totalOperaciones = totalOperaciones;
        this.operaciones = operaciones;
    }

    public BigDecimal getTotalGanado() {
        return totalGanado;
    }

    public void setTotalGanado(BigDecimal totalGanado) {
        this.totalGanado = totalGanado;
    }

    public BigDecimal getTotalGanadoRed() {
        return totalGanadoRed;
    }

    public void setTotalGanadoRed(BigDecimal totalGanadoRed) {
        this.totalGanadoRed = totalGanadoRed;
    }

    public Integer getTotalOperaciones() {
        return totalOperaciones;
    }

    public void setTotalOperaciones(Integer totalOperaciones) {
        this.totalOperaciones = totalOperaciones;
    }

    public List<MyWeeklyCommissionOperationDto> getOperaciones() {
        return operaciones;
    }

    public void setOperaciones(List<MyWeeklyCommissionOperationDto> operaciones) {
        this.operaciones = operaciones;
    }
}