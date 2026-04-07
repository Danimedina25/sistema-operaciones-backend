package com.sistemadeoperaciones.cuentasbancarias.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class BankAccountRequestDto {

    @NotBlank(message = "El banco es obligatorio")
    @Size(max = 100, message = "El banco no puede exceder 100 caracteres")
    private String banco;

    @NotBlank(message = "El titular es obligatorio")
    @Size(max = 150, message = "El titular no puede exceder 150 caracteres")
    private String titular;

    @NotBlank(message = "El número de cuenta es obligatorio")
    @Size(max = 30, message = "El número de cuenta no puede exceder 30 caracteres")
    private String numeroCuenta;

    @NotBlank(message = "La CLABE es obligatoria")
    @Pattern(regexp = "\\d{18}", message = "La CLABE debe tener exactamente 18 dígitos")
    private String clabe;

    private Boolean activo;

    public String getBanco() {
        return banco;
    }

    public String getTitular() {
        return titular;
    }

    public String getNumeroCuenta() {
        return numeroCuenta;
    }

    public String getClabe() {
        return clabe;
    }

    public Boolean getActivo() {
        return activo;
    }

    public void setBanco(String banco) {
        this.banco = banco;
    }

    public void setTitular(String titular) {
        this.titular = titular;
    }

    public void setNumeroCuenta(String numeroCuenta) {
        this.numeroCuenta = numeroCuenta;
    }

    public void setClabe(String clabe) {
        this.clabe = clabe;
    }

    public void setActivo(Boolean activo) {
        this.activo = activo;
    }
}