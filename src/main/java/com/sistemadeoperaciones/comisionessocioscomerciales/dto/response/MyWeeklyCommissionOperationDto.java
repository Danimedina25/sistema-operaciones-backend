package com.sistemadeoperaciones.comisionessocioscomerciales.dto.response;

import com.sistemadeoperaciones.pagos.enums.CommissionStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class MyWeeklyCommissionOperationDto {

    private Long operationId;

    private String cliente;

    private LocalDateTime fechaOperacion;

    private BigDecimal montoOperacion;

    private Integer nivelesRedComercial;

    private BigDecimal porcentajeComision;

    private BigDecimal miComision;

    private BigDecimal comisionRed;

    private BigDecimal comisionNivel2;

    private BigDecimal comisionNivel3;

    private String socioNivel2;

    private String socioNivel3;

    private CommissionStatus myCommissionStatus;

    private CommissionStatus statusNivel2;

    private CommissionStatus statusNivel3;

    public MyWeeklyCommissionOperationDto(Long operationId, String cliente, LocalDateTime fechaOperacion, BigDecimal montoOperacion, Integer nivelesRedComercial, BigDecimal porcentajeComision, BigDecimal miComision, BigDecimal comisionRed, BigDecimal comisionNivel2, BigDecimal comisionNivel3, String socioNivel2, String socioNivel3, CommissionStatus myCommissionStatus, CommissionStatus statusNivel2, CommissionStatus statusNivel3) {
        this.operationId = operationId;
        this.cliente = cliente;
        this.fechaOperacion = fechaOperacion;
        this.montoOperacion = montoOperacion;
        this.nivelesRedComercial = nivelesRedComercial;
        this.porcentajeComision = porcentajeComision;
        this.miComision = miComision;
        this.comisionRed = comisionRed;
        this.comisionNivel2 = comisionNivel2;
        this.comisionNivel3 = comisionNivel3;
        this.socioNivel2 = socioNivel2;
        this.socioNivel3 = socioNivel3;
        this.myCommissionStatus = myCommissionStatus;
        this.statusNivel2 = statusNivel2;
        this.statusNivel3 = statusNivel3;
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

    public LocalDateTime getFechaOperacion() {
        return fechaOperacion;
    }

    public void setFechaOperacion(LocalDateTime fechaOperacion) {
        this.fechaOperacion = fechaOperacion;
    }

    public BigDecimal getMontoOperacion() {
        return montoOperacion;
    }

    public void setMontoOperacion(BigDecimal montoOperacion) {
        this.montoOperacion = montoOperacion;
    }

    public Integer getNivelesRedComercial() {
        return nivelesRedComercial;
    }

    public void setNivelesRedComercial(Integer nivelesRedComercial) {
        this.nivelesRedComercial = nivelesRedComercial;
    }

    public BigDecimal getMiComision() {
        return miComision;
    }

    public void setMiComision(BigDecimal miComision) {
        this.miComision = miComision;
    }

    public BigDecimal getComisionRed() {
        return comisionRed;
    }

    public void setComisionRed(BigDecimal comisionRed) {
        this.comisionRed = comisionRed;
    }

    public BigDecimal getComisionNivel2() {
        return comisionNivel2;
    }

    public void setComisionNivel2(BigDecimal comisionNivel2) {
        this.comisionNivel2 = comisionNivel2;
    }

    public BigDecimal getComisionNivel3() {
        return comisionNivel3;
    }

    public void setComisionNivel3(BigDecimal comisionNivel3) {
        this.comisionNivel3 = comisionNivel3;
    }

    public String getSocioNivel2() {
        return socioNivel2;
    }

    public void setSocioNivel2(String socioNivel2) {
        this.socioNivel2 = socioNivel2;
    }

    public String getSocioNivel3() {
        return socioNivel3;
    }

    public void setSocioNivel3(String socioNivel3) {
        this.socioNivel3 = socioNivel3;
    }

    public CommissionStatus getMyCommissionStatus() {
        return myCommissionStatus;
    }

    public void setMyCommissionStatus(CommissionStatus myCommissionStatus) {
        this.myCommissionStatus = myCommissionStatus;
    }

    public CommissionStatus getStatusNivel2() {
        return statusNivel2;
    }

    public void setStatusNivel2(CommissionStatus statusNivel2) {
        this.statusNivel2 = statusNivel2;
    }

    public CommissionStatus getStatusNivel3() {
        return statusNivel3;
    }

    public void setStatusNivel3(CommissionStatus statusNivel3) {
        this.statusNivel3 = statusNivel3;
    }

    public BigDecimal getPorcentajeComision() {
        return porcentajeComision;
    }

    public void setPorcentajeComision(BigDecimal porcentajeComision) {
        this.porcentajeComision = porcentajeComision;
    }
}