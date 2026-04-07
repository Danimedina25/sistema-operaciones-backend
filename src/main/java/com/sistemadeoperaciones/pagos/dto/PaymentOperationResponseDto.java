package com.sistemadeoperaciones.pagos.dto;

import com.sistemadeoperaciones.pagos.enums.OperationStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public class PaymentOperationResponseDto {

    private Long id;
    private String clienteNombre;
    private String clienteTelefono;
    private BigDecimal montoTotal;
    private BigDecimal montoValidado;
    private BigDecimal saldoPendiente;
    private OperationStatus estatus;
    private Long cuentaDestinoId;
    private String cuentaDestinoBanco;
    private Long socioComercialId;
    private String socioComercialNombre;
    private String observaciones;
    private List<OperationPaymentResponseDto> pagos;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public PaymentOperationResponseDto() {
    }

    public PaymentOperationResponseDto(Long id, String clienteNombre, String clienteTelefono,
                                       BigDecimal montoTotal, BigDecimal montoValidado,
                                       BigDecimal saldoPendiente, OperationStatus estatus,
                                       Long cuentaDestinoId, String cuentaDestinoBanco,
                                       Long socioComercialId, String socioComercialNombre,
                                       String observaciones, List<OperationPaymentResponseDto> pagos,
                                       LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.clienteNombre = clienteNombre;
        this.clienteTelefono = clienteTelefono;
        this.montoTotal = montoTotal;
        this.montoValidado = montoValidado;
        this.saldoPendiente = saldoPendiente;
        this.estatus = estatus;
        this.cuentaDestinoId = cuentaDestinoId;
        this.cuentaDestinoBanco = cuentaDestinoBanco;
        this.socioComercialId = socioComercialId;
        this.socioComercialNombre = socioComercialNombre;
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

    public String getClienteTelefono() {
        return clienteTelefono;
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

    public Long getCuentaDestinoId() {
        return cuentaDestinoId;
    }

    public String getCuentaDestinoBanco() {
        return cuentaDestinoBanco;
    }

    public Long getSocioComercialId() {
        return socioComercialId;
    }

    public String getSocioComercialNombre() {
        return socioComercialNombre;
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

    public void setClienteTelefono(String clienteTelefono) {
        this.clienteTelefono = clienteTelefono;
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

    public void setCuentaDestinoId(Long cuentaDestinoId) {
        this.cuentaDestinoId = cuentaDestinoId;
    }

    public void setCuentaDestinoBanco(String cuentaDestinoBanco) {
        this.cuentaDestinoBanco = cuentaDestinoBanco;
    }

    public void setSocioComercialId(Long socioComercialId) {
        this.socioComercialId = socioComercialId;
    }

    public void setSocioComercialNombre(String socioComercialNombre) {
        this.socioComercialNombre = socioComercialNombre;
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