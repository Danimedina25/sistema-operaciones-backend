package com.sistemadeoperaciones.usuarios.model;

import com.sistemadeoperaciones.usuarios.model.User;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(
    name = "commercial_partner_settings",
    uniqueConstraints = {
        @UniqueConstraint(columnNames = "user_id")
    }
)
public class CommercialPartnerSettings {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Relación 1 a 1 con usuario SOCIO_COMERCIAL
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User usuario;

    @Column(name = "commission_percentage", nullable = false, precision = 5, scale = 2)
    private BigDecimal commissionPercentage;

    @Column(name = "applies_to_network", nullable = false)
    private Boolean appliesToNetwork = true;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "updated_by")
    private User updatedBy;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public CommercialPartnerSettings() {
    }

    // ====== Lifecycle ======

    @PrePersist
    public void prePersist() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // ====== Getters & Setters ======

    public Long getId() {
        return id;
    }

    public User getUsuario() {
        return usuario;
    }

    public BigDecimal getCommissionPercentage() {
        return commissionPercentage;
    }

    public Boolean getAppliesToNetwork() {
        return appliesToNetwork;
    }

    public User getUpdatedBy() {
        return updatedBy;
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

    public void setUsuario(User usuario) {
        this.usuario = usuario;
    }

    public void setCommissionPercentage(BigDecimal commissionPercentage) {
        this.commissionPercentage = commissionPercentage;
    }

    public void setAppliesToNetwork(Boolean appliesToNetwork) {
        this.appliesToNetwork = appliesToNetwork;
    }

    public void setUpdatedBy(User updatedBy) {
        this.updatedBy = updatedBy;
    }
}