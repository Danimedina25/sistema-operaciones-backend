package com.sistemadeoperaciones.pagos.dto.retornos;

import com.sistemadeoperaciones.pagos.enums.PaymentType;
import com.sistemadeoperaciones.pagos.enums.ReturnInstallmentStatus;
import com.sistemadeoperaciones.pagos.enums.ReturnPaymentStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Una parcialidad de una solicitud de retorno.
 */
public class ReturnInstallmentResponseDto {

    private Long id;

    private Long returnRequestId;

    private Long operationId;

    private BigDecimal monto;

    private PaymentType tipoPago;

    private ReturnInstallmentStatus estatus;

    // ---- Datos heredados de la solicitud / operación (para listados) ----

    private BigDecimal returnRequestMonto;

    private ReturnPaymentStatus returnRequestEstatus;

    private String clienteNombre;

    private String socioComercialNombre;

    private String socioComercialTelefono;

    private String autorizadoParaRecibir1;

    private String autorizadoParaRecibir2;

    private String autorizadoParaRecibir3;

    private Long cuentaOrigenId;

    private String cuentaOrigenNombre;

    private String comprobanteUrl;

    private String evidenciaImportePreparadoUrl;

    private String comprobanteEntregaUrl;

    private String codigoRetiroSinTarjeta;

    private LocalDateTime fechaHoraRecoleccion;

    private LocalDateTime fechaRealizacion;

    private LocalDateTime fechaEntrega;

    private LocalDateTime fechaConfirmacion;

    private LocalDateTime fechaCancelacion;

    private String observaciones;

    private Long creadoPorId;

    private String creadoPorNombre;

    private Long realizadoPorId;

    private String realizadoPorNombre;

    private Long entregadoPorId;

    private String entregadoPorNombre;

    private Long canceladoPorId;

    private String canceladoPorNombre;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getReturnRequestId() {
        return returnRequestId;
    }

    public void setReturnRequestId(Long returnRequestId) {
        this.returnRequestId = returnRequestId;
    }

    public Long getOperationId() {
        return operationId;
    }

    public void setOperationId(Long operationId) {
        this.operationId = operationId;
    }

    public BigDecimal getMonto() {
        return monto;
    }

    public void setMonto(BigDecimal monto) {
        this.monto = monto;
    }

    public PaymentType getTipoPago() {
        return tipoPago;
    }

    public void setTipoPago(PaymentType tipoPago) {
        this.tipoPago = tipoPago;
    }

    public ReturnInstallmentStatus getEstatus() {
        return estatus;
    }

    public void setEstatus(ReturnInstallmentStatus estatus) {
        this.estatus = estatus;
    }

    public BigDecimal getReturnRequestMonto() {
        return returnRequestMonto;
    }

    public void setReturnRequestMonto(BigDecimal returnRequestMonto) {
        this.returnRequestMonto = returnRequestMonto;
    }

    public ReturnPaymentStatus getReturnRequestEstatus() {
        return returnRequestEstatus;
    }

    public void setReturnRequestEstatus(ReturnPaymentStatus returnRequestEstatus) {
        this.returnRequestEstatus = returnRequestEstatus;
    }

    public String getClienteNombre() {
        return clienteNombre;
    }

    public void setClienteNombre(String clienteNombre) {
        this.clienteNombre = clienteNombre;
    }

    public String getSocioComercialNombre() {
        return socioComercialNombre;
    }

    public void setSocioComercialNombre(String socioComercialNombre) {
        this.socioComercialNombre = socioComercialNombre;
    }

    public String getSocioComercialTelefono() {
        return socioComercialTelefono;
    }

    public void setSocioComercialTelefono(String socioComercialTelefono) {
        this.socioComercialTelefono = socioComercialTelefono;
    }

    public String getAutorizadoParaRecibir1() {
        return autorizadoParaRecibir1;
    }

    public void setAutorizadoParaRecibir1(String autorizadoParaRecibir1) {
        this.autorizadoParaRecibir1 = autorizadoParaRecibir1;
    }

    public String getAutorizadoParaRecibir2() {
        return autorizadoParaRecibir2;
    }

    public void setAutorizadoParaRecibir2(String autorizadoParaRecibir2) {
        this.autorizadoParaRecibir2 = autorizadoParaRecibir2;
    }

    public String getAutorizadoParaRecibir3() {
        return autorizadoParaRecibir3;
    }

    public void setAutorizadoParaRecibir3(String autorizadoParaRecibir3) {
        this.autorizadoParaRecibir3 = autorizadoParaRecibir3;
    }

    public Long getCuentaOrigenId() {
        return cuentaOrigenId;
    }

    public void setCuentaOrigenId(Long cuentaOrigenId) {
        this.cuentaOrigenId = cuentaOrigenId;
    }

    public String getCuentaOrigenNombre() {
        return cuentaOrigenNombre;
    }

    public void setCuentaOrigenNombre(String cuentaOrigenNombre) {
        this.cuentaOrigenNombre = cuentaOrigenNombre;
    }

    public String getComprobanteUrl() {
        return comprobanteUrl;
    }

    public void setComprobanteUrl(String comprobanteUrl) {
        this.comprobanteUrl = comprobanteUrl;
    }

    public String getEvidenciaImportePreparadoUrl() {
        return evidenciaImportePreparadoUrl;
    }

    public void setEvidenciaImportePreparadoUrl(String evidenciaImportePreparadoUrl) {
        this.evidenciaImportePreparadoUrl = evidenciaImportePreparadoUrl;
    }

    public String getComprobanteEntregaUrl() {
        return comprobanteEntregaUrl;
    }

    public void setComprobanteEntregaUrl(String comprobanteEntregaUrl) {
        this.comprobanteEntregaUrl = comprobanteEntregaUrl;
    }

    public String getCodigoRetiroSinTarjeta() {
        return codigoRetiroSinTarjeta;
    }

    public void setCodigoRetiroSinTarjeta(String codigoRetiroSinTarjeta) {
        this.codigoRetiroSinTarjeta = codigoRetiroSinTarjeta;
    }

    public LocalDateTime getFechaHoraRecoleccion() {
        return fechaHoraRecoleccion;
    }

    public void setFechaHoraRecoleccion(LocalDateTime fechaHoraRecoleccion) {
        this.fechaHoraRecoleccion = fechaHoraRecoleccion;
    }

    public LocalDateTime getFechaRealizacion() {
        return fechaRealizacion;
    }

    public void setFechaRealizacion(LocalDateTime fechaRealizacion) {
        this.fechaRealizacion = fechaRealizacion;
    }

    public LocalDateTime getFechaEntrega() {
        return fechaEntrega;
    }

    public void setFechaEntrega(LocalDateTime fechaEntrega) {
        this.fechaEntrega = fechaEntrega;
    }

    public LocalDateTime getFechaConfirmacion() {
        return fechaConfirmacion;
    }

    public void setFechaConfirmacion(LocalDateTime fechaConfirmacion) {
        this.fechaConfirmacion = fechaConfirmacion;
    }

    public LocalDateTime getFechaCancelacion() {
        return fechaCancelacion;
    }

    public void setFechaCancelacion(LocalDateTime fechaCancelacion) {
        this.fechaCancelacion = fechaCancelacion;
    }

    public String getObservaciones() {
        return observaciones;
    }

    public void setObservaciones(String observaciones) {
        this.observaciones = observaciones;
    }

    public Long getCreadoPorId() {
        return creadoPorId;
    }

    public void setCreadoPorId(Long creadoPorId) {
        this.creadoPorId = creadoPorId;
    }

    public String getCreadoPorNombre() {
        return creadoPorNombre;
    }

    public void setCreadoPorNombre(String creadoPorNombre) {
        this.creadoPorNombre = creadoPorNombre;
    }

    public Long getRealizadoPorId() {
        return realizadoPorId;
    }

    public void setRealizadoPorId(Long realizadoPorId) {
        this.realizadoPorId = realizadoPorId;
    }

    public String getRealizadoPorNombre() {
        return realizadoPorNombre;
    }

    public void setRealizadoPorNombre(String realizadoPorNombre) {
        this.realizadoPorNombre = realizadoPorNombre;
    }

    public Long getEntregadoPorId() {
        return entregadoPorId;
    }

    public void setEntregadoPorId(Long entregadoPorId) {
        this.entregadoPorId = entregadoPorId;
    }

    public String getEntregadoPorNombre() {
        return entregadoPorNombre;
    }

    public void setEntregadoPorNombre(String entregadoPorNombre) {
        this.entregadoPorNombre = entregadoPorNombre;
    }

    public Long getCanceladoPorId() {
        return canceladoPorId;
    }

    public void setCanceladoPorId(Long canceladoPorId) {
        this.canceladoPorId = canceladoPorId;
    }

    public String getCanceladoPorNombre() {
        return canceladoPorNombre;
    }

    public void setCanceladoPorNombre(String canceladoPorNombre) {
        this.canceladoPorNombre = canceladoPorNombre;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
