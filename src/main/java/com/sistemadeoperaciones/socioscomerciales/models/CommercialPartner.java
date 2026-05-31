package com.sistemadeoperaciones.socioscomerciales.models;

import com.sistemadeoperaciones.usuarios.model.User;
import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "commercial_partners")
public class CommercialPartner {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String nombre;

    @Column(name = "cuenta_bancaria", length = 50)
    private String cuentaBancaria;

    @Column(length = 100)
    private String banco;

    @Column(name = "titular_cuenta", length = 150)
    private String titularCuenta;

    @Column(nullable = false)
    private Integer nivel;

    @Column(nullable = false)
    private Boolean activo = true;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User socioComercial;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    public CommercialPartner() {
    }

    public CommercialPartner(Long id, String nombre, String cuentaBancaria, String banco, String titularCuenta, Integer nivel, Boolean activo, User socioComercial, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.nombre = nombre;
        this.cuentaBancaria = cuentaBancaria;
        this.banco = banco;
        this.titularCuenta = titularCuenta;
        this.nivel = nivel;
        this.activo = activo;
        this.socioComercial = socioComercial;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
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

    public Integer getNivel() {
        return nivel;
    }

    public void setNivel(Integer nivel) {
        this.nivel = nivel;
    }

    public Boolean getActivo() {
        return activo;
    }

    public void setActivo(Boolean activo) {
        this.activo = activo;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public User getSocioComercial() {
        return socioComercial;
    }

    public void setSocioComercial(User socioComercial) {
        this.socioComercial = socioComercial;
    }
}