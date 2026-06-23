package com.sistemadeoperaciones.corte.model;

import com.sistemadeoperaciones.cuentasbancarias.models.BankAccount;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "bank_account_daily_cuts",
        uniqueConstraints = {
                @UniqueConstraint(
                        columnNames = {
                                "bank_account_id",
                                "fecha"
                        }
                )
        }
)
public class BankAccountDailyCut {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "bank_account_id",
            nullable = false
    )
    private BankAccount bankAccount;

    @Column(nullable = false)
    private LocalDate fecha;

    @Column(
            name = "saldo_inicial",
            nullable = false,
            precision = 15,
            scale = 2
    )
    private BigDecimal saldoInicial = BigDecimal.ZERO;

    @Column(
            name = "entradas_transferencia",
            nullable = false,
            precision = 15,
            scale = 2
    )
    private BigDecimal entradasTransferencia = BigDecimal.ZERO;

    @Column(
            name = "entradas_deposito",
            nullable = false,
            precision = 15,
            scale = 2
    )
    private BigDecimal entradasDeposito = BigDecimal.ZERO;

    @Column(
            name = "salidas_retornos",
            nullable = false,
            precision = 15,
            scale = 2
    )
    private BigDecimal salidasRetornos = BigDecimal.ZERO;

    @Column(
            name = "salidas_comisiones",
            nullable = false,
            precision = 15,
            scale = 2
    )
    private BigDecimal salidasComisiones = BigDecimal.ZERO;

    @Column(
            name = "total_entradas",
            nullable = false,
            precision = 15,
            scale = 2
    )
    private BigDecimal totalEntradas = BigDecimal.ZERO;

    @Column(
            name = "total_salidas",
            nullable = false,
            precision = 15,
            scale = 2
    )
    private BigDecimal totalSalidas = BigDecimal.ZERO;

    @Column(
            name = "saldo_final",
            nullable = false,
            precision = 15,
            scale = 2
    )
    private BigDecimal saldoFinal = BigDecimal.ZERO;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
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

    public BankAccount getBankAccount() {
        return bankAccount;
    }

    public void setBankAccount(BankAccount bankAccount) {
        this.bankAccount = bankAccount;
    }

    public LocalDate getFecha() {
        return fecha;
    }

    public void setFecha(LocalDate fecha) {
        this.fecha = fecha;
    }

    public BigDecimal getSaldoInicial() {
        return saldoInicial;
    }

    public void setSaldoInicial(BigDecimal saldoInicial) {
        this.saldoInicial = saldoInicial;
    }

    public BigDecimal getEntradasTransferencia() {
        return entradasTransferencia;
    }

    public void setEntradasTransferencia(BigDecimal entradasTransferencia) {
        this.entradasTransferencia = entradasTransferencia;
    }

    public BigDecimal getEntradasDeposito() {
        return entradasDeposito;
    }

    public void setEntradasDeposito(BigDecimal entradasDeposito) {
        this.entradasDeposito = entradasDeposito;
    }

    public BigDecimal getSalidasRetornos() {
        return salidasRetornos;
    }

    public void setSalidasRetornos(BigDecimal salidasRetornos) {
        this.salidasRetornos = salidasRetornos;
    }

    public BigDecimal getSalidasComisiones() {
        return salidasComisiones;
    }

    public void setSalidasComisiones(BigDecimal salidasComisiones) {
        this.salidasComisiones = salidasComisiones;
    }

    public BigDecimal getTotalEntradas() {
        return totalEntradas;
    }

    public void setTotalEntradas(BigDecimal totalEntradas) {
        this.totalEntradas = totalEntradas;
    }

    public BigDecimal getTotalSalidas() {
        return totalSalidas;
    }

    public void setTotalSalidas(BigDecimal totalSalidas) {
        this.totalSalidas = totalSalidas;
    }

    public BigDecimal getSaldoFinal() {
        return saldoFinal;
    }

    public void setSaldoFinal(BigDecimal saldoFinal) {
        this.saldoFinal = saldoFinal;
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
}