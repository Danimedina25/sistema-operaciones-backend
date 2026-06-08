package com.sistemadeoperaciones.usuarios.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class CommercialPartnerSettingsResponseDto {

    private Long id;
    private Long userId;

    private Boolean appliesToNetwork;

    private String cuentaBancaria;
    private String banco;
    private String titularCuenta;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public CommercialPartnerSettingsResponseDto() {
    }

    public CommercialPartnerSettingsResponseDto(Long id, Long userId, Boolean appliesToNetwork, String cuentaBancaria, String banco, String titularCuenta, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.userId = userId;
        this.appliesToNetwork = appliesToNetwork;
        this.cuentaBancaria = cuentaBancaria;
        this.banco = banco;
        this.titularCuenta = titularCuenta;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public Long getId() {
        return id;
    }

    public Long getUserId() {
        return userId;
    }

    public Boolean getAppliesToNetwork() {
        return appliesToNetwork;
    }

    public String getCuentaBancaria() {
        return cuentaBancaria;
    }

    public String getBanco() {
        return banco;
    }

    public String getTitularCuenta() {
        return titularCuenta;
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

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public void setAppliesToNetwork(Boolean appliesToNetwork) {
        this.appliesToNetwork = appliesToNetwork;
    }

    public void setCuentaBancaria(String cuentaBancaria) {
        this.cuentaBancaria = cuentaBancaria;
    }

    public void setBanco(String banco) {
        this.banco = banco;
    }

    public void setTitularCuenta(String titularCuenta) {
        this.titularCuenta = titularCuenta;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}