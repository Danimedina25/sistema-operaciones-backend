package com.sistemadeoperaciones.usuarios.dto.request;

import jakarta.persistence.Column;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public class CreateUserRequestDto {

    @NotBlank(message = "El nombre es obligatorio")
    @Size(max = 150, message = "El nombre no puede exceder 150 caracteres")
    private String nombre;

    @NotBlank(message = "El correo es obligatorio")
    @Email(message = "El correo no tiene un formato válido")
    @Size(max = 150, message = "El correo no puede exceder 150 caracteres")
    private String correo;

    @NotNull(message = "El rol es obligatorio")
    private Long roleId;

    // Solo aplica si el rol es SOCIO_COMERCIAL
    private Boolean appliesToNetwork = true;

    @Size(max = 50, message = "La cuenta bancaria no puede exceder 50 caracteres")
    private String cuentaBancaria;

    @Size(max = 100, message = "El banco no puede exceder 100 caracteres")
    private String banco;

    @Size(max = 150, message = "El titular de la cuenta no puede exceder 150 caracteres")
    private String titularCuenta;

    public CreateUserRequestDto() {
    }

    public CreateUserRequestDto(String nombre, String correo, Long roleId, Boolean appliesToNetwork, String cuentaBancaria, String banco, String titularCuenta) {
        this.nombre = nombre;
        this.correo = correo;
        this.roleId = roleId;
        this.appliesToNetwork = appliesToNetwork;
        this.cuentaBancaria = cuentaBancaria;
        this.banco = banco;
        this.titularCuenta = titularCuenta;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getCorreo() {
        return correo;
    }

    public void setCorreo(String correo) {
        this.correo = correo;
    }

    public Long getRoleId() {
        return roleId;
    }

    public void setRoleId(Long roleId) {
        this.roleId = roleId;
    }

    public Boolean getAppliesToNetwork() {
        return appliesToNetwork;
    }

    public void setAppliesToNetwork(Boolean appliesToNetwork) {
        this.appliesToNetwork = appliesToNetwork;
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
}