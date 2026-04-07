package com.sistemadeoperaciones.cuentasbancarias.dto;

import java.time.LocalDateTime;

public class BankAccountResponseDto {

    private Long id;
    private String banco;
    private String titular;
    private String numeroCuenta;
    private String clabe;
    private Boolean activo;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public BankAccountResponseDto() {
    }

    public BankAccountResponseDto(Long id, String banco, String titular, String numeroCuenta,
                                  String clabe, Boolean activo, LocalDateTime createdAt,
                                  LocalDateTime updatedAt) {
        this.id = id;
        this.banco = banco;
        this.titular = titular;
        this.numeroCuenta = numeroCuenta;
        this.clabe = clabe;
        this.activo = activo;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public Long getId() {
        return id;
    }

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

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setId(Long id) {
        this.id = id;
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

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}