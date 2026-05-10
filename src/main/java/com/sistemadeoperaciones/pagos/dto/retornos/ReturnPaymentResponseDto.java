package com.sistemadeoperaciones.pagos.dto.retornos;

import com.sistemadeoperaciones.pagos.enums.PaymentType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class ReturnPaymentResponseDto {

    private Long id;

    private Long operationId;

    private BigDecimal monto;

    private PaymentType tipoPago;

    private Long cuentaOrigenId;
    private String cuentaOrigenNombre;
    private String cuentaOrigenBanco;

    /**
     * Cuenta o CLABE capturada del cliente
     */
    private String cuentaDestinoCliente;

    private String comprobanteUrl;

    private String observaciones;

    private Long registradoPorId;
    private String registradoPorNombre;

    private LocalDateTime fechaRetorno;

    private LocalDateTime createdAt;

    public ReturnPaymentResponseDto() {
    }

    public Long getId() {
        return id;
    }

    public Long getOperationId() {
        return operationId;
    }

    public BigDecimal getMonto() {
        return monto;
    }

    public PaymentType getTipoPago() {
        return tipoPago;
    }

    public Long getCuentaOrigenId() {
        return cuentaOrigenId;
    }

    public String getCuentaOrigenNombre() {
        return cuentaOrigenNombre;
    }

    public String getCuentaDestinoCliente() {
        return cuentaDestinoCliente;
    }

    public String getComprobanteUrl() {
        return comprobanteUrl;
    }

    public String getObservaciones() {
        return observaciones;
    }

    public Long getRegistradoPorId() {
        return registradoPorId;
    }

    public String getRegistradoPorNombre() {
        return registradoPorNombre;
    }

    public LocalDateTime getFechaRetorno() {
        return fechaRetorno;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setOperationId(Long operationId) {
        this.operationId = operationId;
    }

    public void setMonto(BigDecimal monto) {
        this.monto = monto;
    }

    public void setTipoPago(PaymentType tipoPago) {
        this.tipoPago = tipoPago;
    }

    public void setCuentaOrigenId(Long cuentaOrigenId) {
        this.cuentaOrigenId = cuentaOrigenId;
    }

    public void setCuentaOrigenNombre(String cuentaOrigenNombre) {
        this.cuentaOrigenNombre = cuentaOrigenNombre;
    }

    public void setCuentaDestinoCliente(String cuentaDestinoCliente) {
        this.cuentaDestinoCliente = cuentaDestinoCliente;
    }

    public void setComprobanteUrl(String comprobanteUrl) {
        this.comprobanteUrl = comprobanteUrl;
    }

    public void setObservaciones(String observaciones) {
        this.observaciones = observaciones;
    }

    public void setRegistradoPorId(Long registradoPorId) {
        this.registradoPorId = registradoPorId;
    }

    public void setRegistradoPorNombre(String registradoPorNombre) {
        this.registradoPorNombre = registradoPorNombre;
    }

    public void setFechaRetorno(LocalDateTime fechaRetorno) {
        this.fechaRetorno = fechaRetorno;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public String getCuentaOrigenBanco() {
        return cuentaOrigenBanco;
    }

    public void setCuentaOrigenBanco(String cuentaOrigenBanco) {
        this.cuentaOrigenBanco = cuentaOrigenBanco;
    }
}