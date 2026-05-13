package com.sistemadeoperaciones.pagos.dto;

import com.sistemadeoperaciones.pagos.enums.PaymentType;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public class UpdateOperationPaymentRequestDto {

    @NotNull(message = "El monto es obligatorio")
    @DecimalMin(value = "0.01", message = "El monto debe ser mayor a cero")
    private BigDecimal monto;

    @NotNull(message = "El tipo de pago es obligatorio")
    private PaymentType tipoPago;

    private Long cuentaDestinoId;

    @NotBlank(message = "El comprobante es obligatorio")
    @Size(max = 500, message = "La URL del comprobante no puede exceder 500 caracteres")
    private String comprobanteUrl;

    @Size(max = 500, message = "Las observaciones no pueden exceder 500 caracteres")
    private String observaciones;

    @AssertTrue(message = "La cuenta destino es obligatoria para transferencias y depósitos")
    public boolean isCuentaDestinoValid() {
        if (tipoPago == PaymentType.EFECTIVO) {
            return true;
        }

        return cuentaDestinoId != null && cuentaDestinoId > 0;
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

    public Long getCuentaDestinoId() {
        return cuentaDestinoId;
    }

    public void setCuentaDestinoId(Long cuentaDestinoId) {
        this.cuentaDestinoId = cuentaDestinoId;
    }

    public String getComprobanteUrl() {
        return comprobanteUrl;
    }

    public void setComprobanteUrl(String comprobanteUrl) {
        this.comprobanteUrl = comprobanteUrl;
    }

    public String getObservaciones() {
        return observaciones;
    }

    public void setObservaciones(String observaciones) {
        this.observaciones = observaciones;
    }
}