package com.sistemadeoperaciones.socioscomerciales.dto;

import java.math.BigDecimal;

public class CommercialPartnerResponseDto {

    private Long id;

    private String nombre;

    private String cuentaBancaria;

    private String banco;

    private String titularCuenta;

    private Boolean activo;

    private Long socioComercialId;
    private String socioComercialNombre;

    private BigDecimal porcentajeComision;

    public CommercialPartnerResponseDto() {
    }

    public CommercialPartnerResponseDto(Long id, String nombre, String cuentaBancaria, String banco, String titularCuenta, Boolean activo, Long socioComercialId, String socioComercialNombre, BigDecimal porcentajeComision) {
        this.id = id;
        this.nombre = nombre;
        this.cuentaBancaria = cuentaBancaria;
        this.banco = banco;
        this.titularCuenta = titularCuenta;
        this.activo = activo;
        this.socioComercialId = socioComercialId;
        this.socioComercialNombre = socioComercialNombre;
        this.porcentajeComision = porcentajeComision;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public Long getSocioComercialId() {
        return socioComercialId;
    }

    public void setSocioComercialId(Long socioComercialId) {
        this.socioComercialId = socioComercialId;
    }

    public String getSocioComercialNombre() {
        return socioComercialNombre;
    }

    public void setSocioComercialNombre(String socioComercialNombre) {
        this.socioComercialNombre = socioComercialNombre;
    }

    public BigDecimal getPorcentajeComision() {
        return porcentajeComision;
    }

    public void setPorcentajeComision(BigDecimal porcentajeComision) {
        this.porcentajeComision = porcentajeComision;
    }
}