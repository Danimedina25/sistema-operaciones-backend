package com.sistemadeoperaciones.pagos.dto.retornos;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Alta de una parcialidad para una solicitud de retorno.
 *
 * El {@code tipoPago} no se recibe: se copia de la solicitud. Los campos
 * requeridos dependen del método:
 * <ul>
 *   <li>TRANSFERENCIA / DEPOSITO / CHEQUE → {@code cuentaOrigenId} (solo transferencia)
 *       y {@code comprobanteUrl}. La parcialidad nace COMPLETADA.</li>
 *   <li>EFECTIVO → {@code fechaHoraRecoleccion}. Nace PROGRAMADA.</li>
 *   <li>RETIRO_SIN_TARJETA → {@code fechaHoraRecoleccion} + {@code cuentaOrigenId}
 *       + {@code codigoRetiroSinTarjeta}. Nace PROGRAMADA.</li>
 * </ul>
 */
public class CreateReturnInstallmentRequestDto {

    @NotNull(message = "El monto de la parcialidad es obligatorio")
    @DecimalMin(value = "0.01", message = "El monto debe ser mayor a 0")
    private BigDecimal monto;

    private Long cuentaOrigenId;

    @Size(max = 500, message = "La URL del comprobante no puede exceder 500 caracteres")
    private String comprobanteUrl;

    private LocalDateTime fechaHoraRecoleccion;

    @Size(max = 40, message = "El código de retiro sin tarjeta no puede exceder 40 caracteres")
    private String codigoRetiroSinTarjeta;

    @Size(max = 500, message = "Las observaciones no pueden superar los 500 caracteres")
    private String observaciones;

    public BigDecimal getMonto() {
        return monto;
    }

    public void setMonto(BigDecimal monto) {
        this.monto = monto;
    }

    public Long getCuentaOrigenId() {
        return cuentaOrigenId;
    }

    public void setCuentaOrigenId(Long cuentaOrigenId) {
        this.cuentaOrigenId = cuentaOrigenId;
    }

    public String getComprobanteUrl() {
        return comprobanteUrl;
    }

    public void setComprobanteUrl(String comprobanteUrl) {
        this.comprobanteUrl = comprobanteUrl;
    }

    public LocalDateTime getFechaHoraRecoleccion() {
        return fechaHoraRecoleccion;
    }

    public void setFechaHoraRecoleccion(LocalDateTime fechaHoraRecoleccion) {
        this.fechaHoraRecoleccion = fechaHoraRecoleccion;
    }

    public String getCodigoRetiroSinTarjeta() {
        return codigoRetiroSinTarjeta;
    }

    public void setCodigoRetiroSinTarjeta(String codigoRetiroSinTarjeta) {
        this.codigoRetiroSinTarjeta = codigoRetiroSinTarjeta;
    }

    public String getObservaciones() {
        return observaciones;
    }

    public void setObservaciones(String observaciones) {
        this.observaciones = observaciones;
    }
}
