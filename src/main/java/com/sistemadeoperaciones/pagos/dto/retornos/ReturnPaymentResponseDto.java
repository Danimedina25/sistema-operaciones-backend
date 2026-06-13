package com.sistemadeoperaciones.pagos.dto.retornos;

import com.sistemadeoperaciones.pagos.enums.PaymentType;
import com.sistemadeoperaciones.pagos.enums.ReturnPaymentStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class ReturnPaymentResponseDto {

    private Long id;

    private Long operationId;

    private BigDecimal monto;

    private PaymentType tipoPago;

    private ReturnPaymentStatus estatus;

    /**
     * Cuenta origen de la empresa
     * Se llena cuando el retorno ya fue realizado
     */
    private Long cuentaOrigenId;

    private String cuentaOrigenNombre;

    private String cuentaOrigenBanco;

    /**
     * Cuenta o CLABE del cliente
     */
    private String cuentaDestinoCliente;

    /**
     * Titular de la cuenta del cliente
     */
    private String cuentaDestinoTitular;

    /**
     * Banco de la cuenta del cliente
     */
    private String cuentaDestinoBanco;

    /**
     * Comprobante del retorno realizado
     */
    private String comprobanteUrl;

    private String observaciones;

    /**
     * Usuario que solicitó el retorno
     */
    private Long solicitadoPorId;

    private String solicitadoPorNombre;

    /**
     * Usuario que realizó el retorno
     */
    private Long pagadoPorId;

    private String pagadoPorNombre;

    /**
     * Fecha en que el socio comercial solicitó el retorno
     */
    private LocalDateTime fechaSolicitud;

    /**
     * Fecha en que se realizó el retorno
     */
    private LocalDateTime fechaPago;

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

    public ReturnPaymentStatus getEstatus() {
        return estatus;
    }

    public Long getCuentaOrigenId() {
        return cuentaOrigenId;
    }

    public String getCuentaOrigenNombre() {
        return cuentaOrigenNombre;
    }

    public String getCuentaOrigenBanco() {
        return cuentaOrigenBanco;
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

    public Long getSolicitadoPorId() {
        return solicitadoPorId;
    }

    public String getSolicitadoPorNombre() {
        return solicitadoPorNombre;
    }

    public Long getPagadoPorId() {
        return pagadoPorId;
    }

    public String getPagadoPorNombre() {
        return pagadoPorNombre;
    }

    public LocalDateTime getFechaSolicitud() {
        return fechaSolicitud;
    }

    public LocalDateTime getFechaPago() {
        return fechaPago;
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

    public void setEstatus(ReturnPaymentStatus estatus) {
        this.estatus = estatus;
    }

    public void setCuentaOrigenId(Long cuentaOrigenId) {
        this.cuentaOrigenId = cuentaOrigenId;
    }

    public void setCuentaOrigenNombre(String cuentaOrigenNombre) {
        this.cuentaOrigenNombre = cuentaOrigenNombre;
    }

    public void setCuentaOrigenBanco(String cuentaOrigenBanco) {
        this.cuentaOrigenBanco = cuentaOrigenBanco;
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

    public void setSolicitadoPorId(Long solicitadoPorId) {
        this.solicitadoPorId = solicitadoPorId;
    }

    public void setSolicitadoPorNombre(String solicitadoPorNombre) {
        this.solicitadoPorNombre = solicitadoPorNombre;
    }

    public void setPagadoPorId(Long pagadoPorId) {
        this.pagadoPorId = pagadoPorId;
    }

    public void setPagadoPorNombre(String pagadoPorNombre) {
        this.pagadoPorNombre = pagadoPorNombre;
    }

    public void setFechaSolicitud(LocalDateTime fechaSolicitud) {
        this.fechaSolicitud = fechaSolicitud;
    }

    public void setFechaPago(LocalDateTime fechaPago) {
        this.fechaPago = fechaPago;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public String getCuentaDestinoTitular() {
        return cuentaDestinoTitular;
    }

    public void setCuentaDestinoTitular(String cuentaDestinoTitular) {
        this.cuentaDestinoTitular = cuentaDestinoTitular;
    }

    public String getCuentaDestinoBanco() {
        return cuentaDestinoBanco;
    }

    public void setCuentaDestinoBanco(String cuentaDestinoBanco) {
        this.cuentaDestinoBanco = cuentaDestinoBanco;
    }
}