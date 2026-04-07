package com.sistemadeoperaciones.cuentasbancarias.models;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "bank_accounts",
       uniqueConstraints = {
           @UniqueConstraint(columnNames = "numero_cuenta"),
           @UniqueConstraint(columnNames = "clabe")
       })
public class BankAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String banco;

    @Column(nullable = false, length = 150)
    private String titular;

    @Column(name = "numero_cuenta", nullable = false, length = 255)
    private String numeroCuenta;

    @Column(nullable = false, length = 255)
    private String clabe;

    @Column(nullable = false)
    private Boolean activo = true;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        if (this.activo == null) {
            this.activo = true;
        }
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public BankAccount() {
    }

    public BankAccount(Long id, String banco, String titular, String numeroCuenta, String clabe, Boolean activo) {
        this.id = id;
        this.banco = banco;
        this.titular = titular;
        this.numeroCuenta = numeroCuenta;
        this.clabe = clabe;
        this.activo = activo;
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
}