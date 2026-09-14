package com.sistemadeoperaciones.cajageneral.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import com.sistemadeoperaciones.cajageneral.enums.*;
import com.sistemadeoperaciones.usuarios.model.User;

@Entity
@Table(name = "cash_general_days")
public class CashGeneralDay {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Version
    private Long version;

    @Column(nullable = false, unique = true)
    private LocalDate fecha;

    @Column(precision = 15, scale = 2, nullable = false)
    private BigDecimal saldoInicial;

    @Column(precision = 15, scale = 2, nullable = false)
    private BigDecimal saldoActual;

    @Column(precision = 15, scale = 2)
    private BigDecimal saldoContado;

    @Column(precision = 15, scale = 2)
    private BigDecimal diferencia;

    @ElementCollection
    @CollectionTable(name = "cash_general_opening_counts", joinColumns = @JoinColumn(name = "day_id"))
    @MapKeyEnumerated(EnumType.STRING) @MapKeyColumn(name = "denomination", length = 10)
    @Column(name = "quantity", nullable = false)
    private Map<CashDenomination, Integer> apertura;

    @ElementCollection
    @CollectionTable(name = "cash_general_closing_counts", joinColumns = @JoinColumn(name = "day_id"))
    @MapKeyEnumerated(EnumType.STRING) @MapKeyColumn(name = "denomination", length = 10)
    @Column(name = "quantity", nullable = false)
    private Map<CashDenomination, Integer> cierre;

    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "abierto_por", nullable = false)
    private User abiertoPor;

    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "cerrado_por")
    private User cerradoPor;

    @Column(length = 500)
    private String observacionesCierre;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @Column
    private LocalDateTime closedAt;

    @PrePersist public void prePersist() { createdAt = LocalDateTime.now(); updatedAt = createdAt; }
    @PreUpdate public void preUpdate() { updatedAt = LocalDateTime.now(); }
    public Long getId() { return id; }
    public void setId(Long value) { this.id = value; }
    public Long getVersion() { return version; }
    public void setVersion(Long value) { this.version = value; }
    public LocalDate getFecha() { return fecha; }
    public void setFecha(LocalDate value) { this.fecha = value; }
    public BigDecimal getSaldoInicial() { return saldoInicial; }
    public void setSaldoInicial(BigDecimal value) { this.saldoInicial = value; }
    public BigDecimal getSaldoActual() { return saldoActual; }
    public void setSaldoActual(BigDecimal value) { this.saldoActual = value; }
    public BigDecimal getSaldoContado() { return saldoContado; }
    public void setSaldoContado(BigDecimal value) { this.saldoContado = value; }
    public BigDecimal getDiferencia() { return diferencia; }
    public void setDiferencia(BigDecimal value) { this.diferencia = value; }
    public Map<CashDenomination, Integer> getApertura() { return apertura; }
    public void setApertura(Map<CashDenomination, Integer> value) { this.apertura = value; }
    public Map<CashDenomination, Integer> getCierre() { return cierre; }
    public void setCierre(Map<CashDenomination, Integer> value) { this.cierre = value; }
    public User getAbiertoPor() { return abiertoPor; }
    public void setAbiertoPor(User value) { this.abiertoPor = value; }
    public User getCerradoPor() { return cerradoPor; }
    public void setCerradoPor(User value) { this.cerradoPor = value; }
    public String getObservacionesCierre() { return observacionesCierre; }
    public void setObservacionesCierre(String value) { this.observacionesCierre = value; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime value) { this.createdAt = value; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime value) { this.updatedAt = value; }
    public LocalDateTime getClosedAt() { return closedAt; }
    public void setClosedAt(LocalDateTime value) { this.closedAt = value; }
}
