package com.sistemadeoperaciones.clientes.model;

import com.sistemadeoperaciones.usuarios.model.User;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "clientes")
public class Clientes {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String nombre;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private Boolean activo = true;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "niveles_red_comercial", nullable = false)
    private Integer nivelesRedComercial = 1;

    @Column(name = "porcentaje_comision_socio", nullable = false, precision = 5, scale = 2)
    private BigDecimal porcentajeComisionSocio;

    @Column(name = "porcentaje_comision_oficina", nullable = false, precision = 5, scale = 2)
    private BigDecimal porcentajeComisionOficina = new BigDecimal("1.50");

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();

        if (this.activo == null) {
            this.activo = true;
        }
        if (this.nivelesRedComercial == null) {
            this.nivelesRedComercial = 1;
        }

        if (this.porcentajeComisionSocio == null) {
            this.porcentajeComisionSocio = BigDecimal.ZERO;
        }

        if (this.porcentajeComisionOficina == null) {
            this.porcentajeComisionOficina = new BigDecimal("1.50");
        }
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
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

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
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

    public Integer getNivelesRedComercial() {
        return nivelesRedComercial;
    }

    public void setNivelesRedComercial(Integer nivelesRedComercial) {
        this.nivelesRedComercial = nivelesRedComercial;
    }

    public BigDecimal getPorcentajeComisionSocio() {
        return porcentajeComisionSocio;
    }

    public void setPorcentajeComisionSocio(BigDecimal porcentajeComisionSocio) {
        this.porcentajeComisionSocio = porcentajeComisionSocio;
    }

    public BigDecimal getPorcentajeComisionOficina() {
        return porcentajeComisionOficina;
    }

    public void setPorcentajeComisionOficina(BigDecimal porcentajeComisionOficina) {
        this.porcentajeComisionOficina = porcentajeComisionOficina;
    }
}