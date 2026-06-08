package com.sistemadeoperaciones.socioscomerciales.dto;

import jakarta.validation.constraints.*;

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

}