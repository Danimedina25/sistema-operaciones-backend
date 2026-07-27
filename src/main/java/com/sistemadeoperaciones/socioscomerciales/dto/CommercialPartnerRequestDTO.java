package com.sistemadeoperaciones.socioscomerciales.dto;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public class CommercialPartnerRequestDTO {

    @NotBlank(message = "El nombre es obligatorio")
    @Size(max = 150)
    private String nombre;

    @NotBlank(message = "La cuenta bancaria es obligatoria")
    @Size(max = 50)
    private String cuentaBancaria;

    @NotBlank(message = "El banco es obligatorio")
    @Size(max = 100)
    private String banco;

    @NotBlank(message = "El titular de la cuenta es obligatorio")
    @Size(max = 150)
    private String titularCuenta;

    private Boolean activo = true;

    @NotNull(message = "El porcentaje de comisión es obligatorio")
    @DecimalMin(value = "0.00", message = "El porcentaje de comisión no puede ser negativo")
    @DecimalMax(value = "100.00", message = "El porcentaje de comisión no puede ser mayor a 100")
    private BigDecimal porcentajeComision;

    public CommercialPartnerRequestDTO() {
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }


    public String getCuentaBancaria() {
        return cuentaBancaria;
    }

    public void setCuentaBancaria(String cuentaBancaria) {
        this.cuentaBancaria = cuentaBancaria;
    }

    public String getBanco() {
        return banco;
    }

    public void setBanco(String banco) {
        this.banco = banco;
    }

    public String getTitularCuenta() {
        return titularCuenta;
    }

    public void setTitularCuenta(String titularCuenta) {
        this.titularCuenta = titularCuenta;
    }

    public Boolean getActivo() {
        return activo;
    }

    public void setActivo(Boolean activo) {
        this.activo = activo;
    }

    public BigDecimal getPorcentajeComision() {
        return porcentajeComision;
    }

    public void setPorcentajeComision(BigDecimal porcentajeComision) {
        this.porcentajeComision = porcentajeComision;
    }

}