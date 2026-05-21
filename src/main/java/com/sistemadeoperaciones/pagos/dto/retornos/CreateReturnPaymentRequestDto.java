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

    @Size(max = 80, message = "El banco no puede superar los 80 caracteres")
    private String banco;

    @Size(max = 150, message = "El titular no puede superar los 150 caracteres")
    private String titular;

    @Size(max = 30, message = "La cuenta no puede superar los 30 caracteres")
    private String cuenta;

    @Size(max = 18, message = "La CLABE no puede superar los 18 caracteres")
    private String clabe;

    @Size(max = 500, message = "Las observaciones no pueden superar los 500 caracteres")
    private String observaciones;

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

    public String getBanco() {
        return banco;
    }

    public void setBanco(String banco) {
        this.banco = banco;
    }

    public String getTitular() {
        return titular;
    }

    public void setTitular(String titular) {
        this.titular = titular;
    }

    public String getCuenta() {
        return cuenta;
    }

    public void setCuenta(String cuenta) {
        this.cuenta = cuenta;
    }

    public String getClabe() {
        return clabe;
    }

    public void setClabe(String clabe) {
        this.clabe = clabe;
    }

    public String getObservaciones() {
        return observaciones;
    }

    public void setObservaciones(String observaciones) {
        this.observaciones = observaciones;
    }
}