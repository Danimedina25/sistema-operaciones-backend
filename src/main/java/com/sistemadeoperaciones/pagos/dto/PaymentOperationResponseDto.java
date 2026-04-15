package com.sistemadeoperaciones.pagos.dto;

import com.sistemadeoperaciones.pagos.enums.OperationStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public class PaymentOperationResponseDto {

    private Long id;
    private String clienteNombre;
    private BigDecimal montoTotal;
    private BigDecimal montoValidado;
    private BigDecimal saldoPendiente;
    private OperationStatus estatus;
    private Long socioComercialId;
    private String socioComercialNombre;
    private Integer nivelesRedComercial;
    private BigDecimal porcentajeComisionAplicado;
    private String observaciones;
    private List<OperationPaymentResponseDto> pagos;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public PaymentOperationResponseDto() {
    }

    public PaymentOperationResponseDto(Long id, String clienteNombre,
                                       BigDecimal montoTotal, BigDecimal montoValidado,
                                       BigDecimal saldoPendiente, OperationStatus estatus,
                                       Long socioComercialId, String socioComercialNombre,
                                       Integer nivelesRedComercial, BigDecimal porcentajeComisionAplicado,
                                       String observaciones, List<OperationPaymentResponseDto> pagos,
                                       LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.clienteNombre = clienteNombre;
        this.montoTotal = montoTotal;
        this.montoValidado = montoValidado;
        this.saldoPendiente = saldoPendiente;
        this.estatus = estatus;
        this.socioComercialId = socioComercialId;
        this.socioComercialNombre = socioComercialNombre;
        this.nivelesRedComercial = nivelesRedComercial;
        this.porcentajeComisionAplicado = porcentajeComisionAplicado;
        this.observaciones = observaciones;
        this.pagos = pagos;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public Long getId() {
        return id;
    }

    public String getClienteNombre() {
        return clienteNombre;
    }

    public BigDecimal getMontoTotal() {
        return montoTotal;
    }

    public BigDecimal getMontoValidado() {
        return montoValidado;
    }

    public BigDecimal getSaldoPendiente() {
        return saldoPendiente;
    }

    public OperationStatus getEstatus() {
        return estatus;
    }

    public Long getSocioComercialId() {
        return socioComercialId;
    }

    public String getSocioComercialNombre() {
        return socioComercialNombre;
    }

    public Integer getNivelesRedComercial() {
        return nivelesRedComercial;
    }

    public BigDecimal getPorcentajeComisionAplicado() {
        return porcentajeComisionAplicado;
    }

    public String getObservaciones() {
        return observaciones;
    }

    public List<OperationPaymentResponseDto> getPagos() {
        return pagos;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setClienteNombre(String clienteNombre) {
        this.clienteNombre = clienteNombre;
    }

    public void setMontoTotal(BigDecimal montoTotal) {
        this.montoTotal = montoTotal;
    }

    public void setMontoValidado(BigDecimal montoValidado) {
        this.montoValidado = montoValidado;
    }

    public void setSaldoPendiente(BigDecimal saldoPendiente) {
        this.saldoPendiente = saldoPendiente;
    }

    public void setEstatus(OperationStatus estatus) {
        this.estatus = estatus;
    }

    public void setSocioComercialId(Long socioComercialId) {
        this.socioComercialId = socioComercialId;
    }

    public void setSocioComercialNombre(String socioComercialNombre) {
        this.socioComercialNombre = socioComercialNombre;
    }

    public void setNivelesRedComercial(Integer nivelesRedComercial) {
        this.nivelesRedComercial = nivelesRedComercial;
    }

    public void setPorcentajeComisionAplicado(BigDecimal porcentajeComisionAplicado) {
        this.porcentajeComisionAplicado = porcentajeComisionAplicado;
    }

    public void setObservaciones(String observaciones) {
        this.observaciones = observaciones;
    }

    public void setPagos(List<OperationPaymentResponseDto> pagos) {
        this.pagos = pagos;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}