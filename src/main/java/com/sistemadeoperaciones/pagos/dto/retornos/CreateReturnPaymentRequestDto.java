package com.sistemadeoperaciones.pagos.dto.retornos;

import com.sistemadeoperaciones.pagos.enums.PaymentType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public class CreateReturnPaymentRequestDto {

    @NotNull(message = "El monto del retorno es obligatorio")
    @DecimalMin(value = "0.01", message = "El monto debe ser mayor a 0")
    private BigDecimal monto;

    @NotNull(message = "El tipo de pago es obligatorio")
    private PaymentType tipoPago;

    /**
     * Nullable cuando es EFECTIVO
     */
    private Long cuentaOrigenId;

    /**
     * Cuenta o CLABE capturada del cliente.
     * Nullable cuando es EFECTIVO.
     */
    @Size(max = 30, message = "La cuenta o CLABE no puede superar los 30 caracteres")
    private String cuentaDestinoCliente;

    /**
     * Opcional para EFECTIVO
     */
    private String comprobanteUrl;

    @Size(max = 500, message = "Las observaciones no pueden superar los 500 caracteres")
    private String observaciones;

    public CreateReturnPaymentRequestDto() {
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

    public String getCuentaDestinoCliente() {
        return cuentaDestinoCliente;
    }

    public String getComprobanteUrl() {
        return comprobanteUrl;
    }

    public String getObservaciones() {
        return observaciones;
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

    public void setCuentaDestinoCliente(String cuentaDestinoCliente) {
        this.cuentaDestinoCliente = cuentaDestinoCliente;
    }

    public void setComprobanteUrl(String comprobanteUrl) {
        this.comprobanteUrl = comprobanteUrl;
    }

    public void setObservaciones(String observaciones) {
        this.observaciones = observaciones;
    }
}