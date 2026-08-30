package com.sistemadeoperaciones.pagos.dto;

import com.sistemadeoperaciones.pagos.enums.PaymentStatus;
import com.sistemadeoperaciones.pagos.enums.PaymentType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class OperationPaymentResponseDto {

    private Long id;
    private BigDecimal monto;
    private PaymentType tipoPago;
    private String comprobanteUrl;
    private String comprobanteValidacionUrl;
    private Long cuentaDestinoId;
    private String cuentaDestinoBanco;
    private String cuentaDestinoTitular;
    private PaymentStatus estatus;
    private String observaciones;
    private Long registradoPorId;
    private String registradoPorNombre;
    private Long validadoPorId;
    private String validadoPorNombre;
    private Long enProcesoPorId;
    private String enProcesoPorNombre;
    private LocalDateTime fechaEnProceso;
    private LocalDateTime fechaPago;
    private LocalDateTime fechaValidacion;
    private LocalDateTime fechaComprobante;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public OperationPaymentResponseDto() {
    }

    public OperationPaymentResponseDto(
            Long id,
            BigDecimal monto,
            PaymentType tipoPago,
            String comprobanteUrl,
            String comprobanteValidacionUrl,
            Long cuentaDestinoId,
            String cuentaDestinoBanco,
            String cuentaDestinoTitular,
            PaymentStatus estatus,
            String observaciones,
            Long registradoPorId,
            String registradoPorNombre,
            Long validadoPorId,
            String validadoPorNombre,
            LocalDateTime fechaPago,
            LocalDateTime fechaValidacion,
            LocalDateTime fechaComprobante,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
        this.id = id;
        this.monto = monto;
        this.tipoPago = tipoPago;
        this.comprobanteUrl = comprobanteUrl;
        this.comprobanteValidacionUrl = comprobanteValidacionUrl;
        this.cuentaDestinoId = cuentaDestinoId;
        this.cuentaDestinoBanco = cuentaDestinoBanco;
        this.cuentaDestinoTitular = cuentaDestinoTitular;
        this.estatus = estatus;
        this.observaciones = observaciones;
        this.registradoPorId = registradoPorId;
        this.registradoPorNombre = registradoPorNombre;
        this.validadoPorId = validadoPorId;
        this.validadoPorNombre = validadoPorNombre;
        this.fechaPago = fechaPago;
        this.fechaValidacion = fechaValidacion;
        this.fechaComprobante = fechaComprobante;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public Long getId() {
        return id;
    }

    public BigDecimal getMonto() {
        return monto;
    }

    public PaymentType getTipoPago() {
        return tipoPago;
    }

    public String getComprobanteUrl() {
        return comprobanteUrl;
    }

    public String getComprobanteValidacionUrl() {
        return comprobanteValidacionUrl;
    }

    public Long getCuentaDestinoId() {
        return cuentaDestinoId;
    }

    public String getCuentaDestinoBanco() {
        return cuentaDestinoBanco;
    }

    public String getCuentaDestinoTitular() {
        return cuentaDestinoTitular;
    }

    public PaymentStatus getEstatus() {
        return estatus;
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

    public Long getValidadoPorId() {
        return validadoPorId;
    }

    public String getValidadoPorNombre() {
        return validadoPorNombre;
    }

    public LocalDateTime getFechaPago() {
        return fechaPago;
    }

    public LocalDateTime getFechaValidacion() {
        return fechaValidacion;
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

    public void setMonto(BigDecimal monto) {
        this.monto = monto;
    }

    public void setTipoPago(PaymentType tipoPago) {
        this.tipoPago = tipoPago;
    }

    public void setComprobanteUrl(String comprobanteUrl) {
        this.comprobanteUrl = comprobanteUrl;
    }

    public void setComprobanteValidacionUrl(String comprobanteValidacionUrl) {
        this.comprobanteValidacionUrl = comprobanteValidacionUrl;
    }

    public void setCuentaDestinoId(Long cuentaDestinoId) {
        this.cuentaDestinoId = cuentaDestinoId;
    }

    public void setCuentaDestinoBanco(String cuentaDestinoBanco) {
        this.cuentaDestinoBanco = cuentaDestinoBanco;
    }

    public void setCuentaDestinoTitular(String cuentaDestinoTitular) {
        this.cuentaDestinoTitular = cuentaDestinoTitular;
    }

    public void setEstatus(PaymentStatus estatus) {
        this.estatus = estatus;
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

    public void setValidadoPorId(Long validadoPorId) {
        this.validadoPorId = validadoPorId;
    }

    public void setValidadoPorNombre(String validadoPorNombre) {
        this.validadoPorNombre = validadoPorNombre;
    }

    public Long getEnProcesoPorId() {
        return enProcesoPorId;
    }

    public void setEnProcesoPorId(Long enProcesoPorId) {
        this.enProcesoPorId = enProcesoPorId;
    }

    public String getEnProcesoPorNombre() {
        return enProcesoPorNombre;
    }

    public void setEnProcesoPorNombre(String enProcesoPorNombre) {
        this.enProcesoPorNombre = enProcesoPorNombre;
    }

    public LocalDateTime getFechaEnProceso() {
        return fechaEnProceso;
    }

    public void setFechaEnProceso(LocalDateTime fechaEnProceso) {
        this.fechaEnProceso = fechaEnProceso;
    }

    public void setFechaPago(LocalDateTime fechaPago) {
        this.fechaPago = fechaPago;
    }

    public void setFechaValidacion(LocalDateTime fechaValidacion) {
        this.fechaValidacion = fechaValidacion;
    }

    public LocalDateTime getFechaComprobante() {
        return fechaComprobante;
    }

    public void setFechaComprobante(LocalDateTime fechaComprobante) {
        this.fechaComprobante = fechaComprobante;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}