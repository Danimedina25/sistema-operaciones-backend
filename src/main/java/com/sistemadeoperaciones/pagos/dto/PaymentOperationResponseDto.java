package com.sistemadeoperaciones.pagos.dto;

import com.sistemadeoperaciones.pagos.enums.OperationStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public class PaymentOperationResponseDto {

    private Long id;
    private Long clienteId;
    private String clienteNombre;
    private BigDecimal montoTotal;
    private BigDecimal montoValidado;
    private BigDecimal saldoPendiente;
    private OperationStatus estatus;
    private Long socioComercialId;
    private String socioComercialNombre;
    private Integer nivelesRedComercial;
    private BigDecimal porcentajeComisionAplicado;
    private BigDecimal porcentajeComisionOficina;

    private BigDecimal porcentajeComisionRedTotal;
    private BigDecimal montoComisionRedTotal;
    private BigDecimal porcentajeComisionOficinaTotal;
    private BigDecimal montoComisionOficinaTotal;
    private BigDecimal montoTotalDevolverCliente;

    private String observaciones;
    private List<OperationPaymentResponseDto> pagos;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public PaymentOperationResponseDto() {
    }

    public PaymentOperationResponseDto(
            Long id,
            Long clienteId,
            String clienteNombre,
            BigDecimal montoTotal,
            BigDecimal montoValidado,
            BigDecimal saldoPendiente,
            OperationStatus estatus,
            Long socioComercialId,
            String socioComercialNombre,
            Integer nivelesRedComercial,
            BigDecimal porcentajeComisionAplicado,
            BigDecimal porcentajeComisionOficina,
            BigDecimal porcentajeComisionRedTotal,
            BigDecimal montoComisionRedTotal,
            BigDecimal porcentajeComisionOficinaTotal,
            BigDecimal montoComisionOficinaTotal,
            BigDecimal montoTotalDevolverCliente,
            String observaciones,
            List<OperationPaymentResponseDto> pagos,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
        this.id = id;
        this.clienteId = clienteId;
        this.clienteNombre = clienteNombre;
        this.montoTotal = montoTotal;
        this.montoValidado = montoValidado;
        this.saldoPendiente = saldoPendiente;
        this.estatus = estatus;
        this.socioComercialId = socioComercialId;
        this.socioComercialNombre = socioComercialNombre;
        this.nivelesRedComercial = nivelesRedComercial;
        this.porcentajeComisionAplicado = porcentajeComisionAplicado;
        this.porcentajeComisionOficina = porcentajeComisionOficina;
        this.porcentajeComisionRedTotal = porcentajeComisionRedTotal;
        this.montoComisionRedTotal = montoComisionRedTotal;
        this.porcentajeComisionOficinaTotal = porcentajeComisionOficinaTotal;
        this.montoComisionOficinaTotal = montoComisionOficinaTotal;
        this.montoTotalDevolverCliente = montoTotalDevolverCliente;
        this.observaciones = observaciones;
        this.pagos = pagos;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public Long getId() {
        return id;
    }

    public Long getClienteId() {
        return clienteId;
    }

    public void setClienteId(Long clienteId) {
        this.clienteId = clienteId;
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

    public BigDecimal getPorcentajeComisionOficina() {
        return porcentajeComisionOficina;
    }

    public BigDecimal getPorcentajeComisionRedTotal() {
        return porcentajeComisionRedTotal;
    }

    public BigDecimal getMontoComisionRedTotal() {
        return montoComisionRedTotal;
    }

    public BigDecimal getPorcentajeComisionOficinaTotal() {
        return porcentajeComisionOficinaTotal;
    }

    public BigDecimal getMontoComisionOficinaTotal() {
        return montoComisionOficinaTotal;
    }

    public BigDecimal getMontoTotalDevolverCliente() {
        return montoTotalDevolverCliente;
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

    public void setPorcentajeComisionOficina(BigDecimal porcentajeComisionOficina) {
        this.porcentajeComisionOficina = porcentajeComisionOficina;
    }

    public void setPorcentajeComisionRedTotal(BigDecimal porcentajeComisionRedTotal) {
        this.porcentajeComisionRedTotal = porcentajeComisionRedTotal;
    }

    public void setMontoComisionRedTotal(BigDecimal montoComisionRedTotal) {
        this.montoComisionRedTotal = montoComisionRedTotal;
    }

    public void setPorcentajeComisionOficinaTotal(BigDecimal porcentajeComisionOficinaTotal) {
        this.porcentajeComisionOficinaTotal = porcentajeComisionOficinaTotal;
    }

    public void setMontoComisionOficinaTotal(BigDecimal montoComisionOficinaTotal) {
        this.montoComisionOficinaTotal = montoComisionOficinaTotal;
    }

    public void setMontoTotalDevolverCliente(BigDecimal montoTotalDevolverCliente) {
        this.montoTotalDevolverCliente = montoTotalDevolverCliente;
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